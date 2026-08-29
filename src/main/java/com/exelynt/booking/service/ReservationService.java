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
import java.util.ArrayList;
import java.util.List;

@Service
public class ReservationService {
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
        validateTimes(request);
        Resource resource = resourceService.getEntity(request.resourceId());
        if (!resource.isAvailable()) {
            throw new IllegalArgumentException("Resource is not available for booking");
        }

        Reservation reservation = new Reservation();
        reservation.setResource(resource);
        reservation.setUser(getManagedUser(requester.getId()));
        reservation.setStartTime(request.startTime());
        reservation.setEndTime(request.endTime());
        reservation.setStatus(requester.getRole() == Role.ADMIN && request.status() != null
                ? request.status()
                : ReservationStatus.PENDING);
        reservation.setPrice(calculatePrice(resource, request));
        return toResponse(reservationRepository.save(reservation));
    }

    @Transactional
    public ReservationResponse updateStatus(Long id, ReservationUpdateRequest request, User requester) {
        Reservation reservation = getEntity(id);
        if (requester.getRole() != Role.ADMIN) {
            throw new ForbiddenException("Only admins can update reservation status");
        }
        reservation.setStatus(request.status());
        return toResponse(reservation);
    }

    @Transactional
    public ReservationResponse update(Long id, ReservationAdminUpdateRequest request, User requester) {
        if (requester.getRole() != Role.ADMIN) {
            throw new ForbiddenException("Only admins can update reservations");
        }
        if (!request.endTime().isAfter(request.startTime())) {
            throw new IllegalArgumentException("Reservation endTime must be after startTime");
        }
        Resource resource = resourceService.getEntity(request.resourceId());
        if (!resource.isAvailable()) {
            throw new IllegalArgumentException("Resource is not available for booking");
        }
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
            throw new ForbiddenException("Only admins can delete reservations");
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

    private void validateTimes(ReservationRequest request) {
        if (!request.endTime().isAfter(request.startTime())) {
            throw new IllegalArgumentException("Reservation endTime must be after startTime");
        }
    }

    private BigDecimal calculatePrice(Resource resource, ReservationRequest request) {
        return calculatePrice(resource, request.startTime(), request.endTime());
    }

    private BigDecimal calculatePrice(Resource resource, java.time.LocalDateTime startTime, java.time.LocalDateTime endTime) {
        long minutes = Duration.between(startTime, endTime).toMinutes();
        BigDecimal hours = BigDecimal.valueOf(minutes).divide(BigDecimal.valueOf(60), 2, RoundingMode.CEILING);
        return resource.getPricePerHour().multiply(hours).setScale(2, RoundingMode.HALF_UP);
    }

    private void ensureAdminOrOwner(Reservation reservation, User requester) {
        boolean owner = reservation.getUser().getId().equals(requester.getId());
        if (requester.getRole() != Role.ADMIN && !owner) {
            throw new ForbiddenException("You can access only your own reservations");
        }
    }

    private Reservation getEntity(Long id) {
        return reservationRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Reservation not found"));
    }

    private User getManagedUser(Long id) {
        return userRepository.findById(id).orElseThrow(() -> new NotFoundException("User not found"));
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
