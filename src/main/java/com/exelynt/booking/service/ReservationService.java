package com.exelynt.booking.service;

import com.exelynt.booking.dto.ReservationAdminUpdateRequest;
import com.exelynt.booking.dto.ReservationRequest;
import com.exelynt.booking.dto.ReservationResponse;
import com.exelynt.booking.dto.ReservationUpdateRequest;
import com.exelynt.booking.entity.Reservation;
import com.exelynt.booking.entity.ReservationStatus;
import com.exelynt.booking.entity.Resource;
import com.exelynt.booking.entity.Role;
import com.exelynt.booking.entity.User;
import com.exelynt.booking.exception.ApiMessages;
import com.exelynt.booking.exception.ForbiddenException;
import com.exelynt.booking.exception.NotFoundException;
import com.exelynt.booking.repository.ReservationRepository;
import com.exelynt.booking.repository.UserRepository;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class ReservationService {
    private static final int PRICE_SCALE = 2;
    private static final int MINUTES_PER_HOUR = 60;

    private final ReservationRepository reservationRepository;
    private final ResourceService resourceService;
    private final UserRepository userRepository;

    public ReservationService(ReservationRepository reservationRepository,
                              ResourceService resourceService,
                              UserRepository userRepository) {
        this.reservationRepository = reservationRepository;
        this.resourceService = resourceService;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public Page<ReservationResponse> findReservations(User requester,
                                                      ReservationStatus status,
                                                      BigDecimal minPrice,
                                                      BigDecimal maxPrice,
                                                      Pageable pageable) {
        Specification<Reservation> specification = filters(requester, status, minPrice, maxPrice);
        return reservationRepository.findAll(specification, pageable).map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public ReservationResponse findById(Long id, User requester) {
        Reservation reservation = getEntity(id);
        ensureAdminOrOwner(reservation, requester);
        return toResponse(reservation);
    }

    @Transactional
    public ReservationResponse create(ReservationRequest request, User requester) {
        validateTimes(request.startTime(), request.endTime());
        Resource resource = getBookableResource(request.resourceId());
        Reservation reservation = buildReservation(request, requester, resource);
        return toResponse(reservationRepository.save(reservation));
    }

    @Transactional
    public ReservationResponse updateStatus(Long id, ReservationUpdateRequest request, User requester) {
        Reservation reservation = getEntity(id);
        if (requester.getRole() != Role.ADMIN) {
            throw new ForbiddenException(ApiMessages.ADMIN_UPDATE_RESERVATION_STATUS_ONLY);
        }
        reservation.setStatus(request.status());
        return toResponse(reservation);
    }

    @Transactional
    public ReservationResponse update(Long id, ReservationAdminUpdateRequest request, User requester) {
        if (requester.getRole() != Role.ADMIN) {
            throw new ForbiddenException(ApiMessages.ADMIN_UPDATE_RESERVATIONS_ONLY);
        }
        validateTimes(request.startTime(), request.endTime());
        Resource resource = getBookableResource(request.resourceId());
        Reservation reservation = getEntity(id);
        reservation.setResource(resource);
        reservation.setStartTime(request.startTime());
        reservation.setEndTime(request.endTime());
        reservation.setStatus(request.status());
        reservation.setPrice(calculatePrice(resource, request.startTime(), request.endTime()));
        return toResponse(reservation);
    }

    @Transactional
    public ReservationResponse cancelOwnReservation(Long id, User requester) {
        Reservation reservation = getEntity(id);
        ensureAdminOrOwner(reservation, requester);
        reservation.setStatus(ReservationStatus.CANCELLED);
        return toResponse(reservation);
    }

    @Transactional
    public void delete(Long id, User requester) {
        if (requester.getRole() != Role.ADMIN) {
            throw new ForbiddenException(ApiMessages.ADMIN_DELETE_RESERVATIONS_ONLY);
        }
        reservationRepository.delete(getEntity(id));
    }

    private Specification<Reservation> filters(User requester,
                                               ReservationStatus status,
                                               BigDecimal minPrice,
                                               BigDecimal maxPrice) {
        return (root, query, builder) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (requester.getRole() == Role.USER) {
                predicates.add(builder.equal(root.get("user").get("id"), requester.getId()));
            }
            if (status != null) {
                predicates.add(builder.equal(root.get("status"), status));
            }
            if (minPrice != null) {
                predicates.add(builder.greaterThanOrEqualTo(root.get("price"), minPrice));
            }
            if (maxPrice != null) {
                predicates.add(builder.lessThanOrEqualTo(root.get("price"), maxPrice));
            }
            return builder.and(predicates.toArray(new Predicate[0]));
        };
    }

    private void validateTimes(LocalDateTime startTime, LocalDateTime endTime) {
        if (!endTime.isAfter(startTime)) {
            throw new IllegalArgumentException(ApiMessages.RESERVATION_END_AFTER_START);
        }
    }

    private Resource getBookableResource(Long resourceId) {
        Resource resource = resourceService.getEntity(resourceId);
        if (!resource.isAvailable()) {
            throw new IllegalArgumentException(ApiMessages.RESOURCE_NOT_AVAILABLE);
        }
        return resource;
    }

    private Reservation buildReservation(ReservationRequest request, User requester, Resource resource) {
        Reservation reservation = new Reservation();
        reservation.setResource(resource);
        reservation.setUser(getManagedUser(requester.getId()));
        reservation.setStartTime(request.startTime());
        reservation.setEndTime(request.endTime());
        reservation.setStatus(resolveInitialStatus(request, requester));
        reservation.setPrice(calculatePrice(resource, request));
        return reservation;
    }

    private ReservationStatus resolveInitialStatus(ReservationRequest request, User requester) {
        if (requester.getRole() == Role.ADMIN && request.status() != null) {
            return request.status();
        }
        return ReservationStatus.PENDING;
    }

    private BigDecimal calculatePrice(Resource resource, ReservationRequest request) {
        return calculatePrice(resource, request.startTime(), request.endTime());
    }

    private BigDecimal calculatePrice(Resource resource, LocalDateTime startTime, LocalDateTime endTime) {
        long minutes = Duration.between(startTime, endTime).toMinutes();
        BigDecimal hours = BigDecimal.valueOf(minutes)
                .divide(BigDecimal.valueOf(MINUTES_PER_HOUR), PRICE_SCALE, RoundingMode.CEILING);
        return resource.getPricePerHour().multiply(hours).setScale(PRICE_SCALE, RoundingMode.HALF_UP);
    }

    private void ensureAdminOrOwner(Reservation reservation, User requester) {
        boolean owner = reservation.getUser().getId().equals(requester.getId());
        if (requester.getRole() != Role.ADMIN && !owner) {
            throw new ForbiddenException(ApiMessages.ACCESS_OWN_RESERVATIONS_ONLY);
        }
    }

    private Reservation getEntity(Long id) {
        return reservationRepository.findById(id)
                .orElseThrow(() -> new NotFoundException(ApiMessages.RESERVATION_NOT_FOUND));
    }

    private User getManagedUser(Long id) {
        return userRepository.findById(id).orElseThrow(() -> new NotFoundException(ApiMessages.USER_NOT_FOUND));
    }

    private ReservationResponse toResponse(Reservation reservation) {
        return new ReservationResponse(
                reservation.getId(),
                reservation.getResource().getId(),
                reservation.getResource().getName(),
                reservation.getUser().getId(),
                reservation.getUser().getEmail(),
                reservation.getStartTime(),
                reservation.getEndTime(),
                reservation.getStatus(),
                reservation.getPrice(),
                reservation.getCreatedAt()
        );
    }
}
