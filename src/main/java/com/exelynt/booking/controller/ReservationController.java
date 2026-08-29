package com.exelynt.booking.controller;

import com.exelynt.booking.dto.ReservationAdminUpdateRequest;
import com.exelynt.booking.dto.ReservationRequest;
import com.exelynt.booking.dto.ReservationResponse;
import com.exelynt.booking.dto.ReservationUpdateRequest;
import com.exelynt.booking.entity.ReservationStatus;
import com.exelynt.booking.entity.User;
import com.exelynt.booking.service.ReservationService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;

@RestController
@RequestMapping("/reservations")
public class ReservationController {
    private final ReservationService reservationService;

    public ReservationController(ReservationService reservationService) {
        this.reservationService = reservationService;
    }

    @GetMapping
    public Page<ReservationResponse> all(@AuthenticationPrincipal User requester,
                                         @RequestParam(required = false) ReservationStatus status,
                                         @RequestParam(required = false) BigDecimal minPrice,
                                         @RequestParam(required = false) BigDecimal maxPrice,
                                         @PageableDefault(size = 10, sort = "createdAt") Pageable pageable) {
        return reservationService.findReservations(requester, status, minPrice, maxPrice, pageable);
    }

    @GetMapping("/{id}")
    public ReservationResponse byId(@PathVariable Long id, @AuthenticationPrincipal User requester) {
        return reservationService.findById(id, requester);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ReservationResponse create(@Valid @RequestBody ReservationRequest request,
                                      @AuthenticationPrincipal User requester) {
        return reservationService.create(request, requester);
    }

    @PutMapping("/{id}/status")
    public ReservationResponse updateStatus(@PathVariable Long id,
                                            @Valid @RequestBody ReservationUpdateRequest request,
                                            @AuthenticationPrincipal User requester) {
        return reservationService.updateStatus(id, request, requester);
    }

    @PutMapping("/{id}")
    public ReservationResponse update(@PathVariable Long id,
                                      @Valid @RequestBody ReservationAdminUpdateRequest request,
                                      @AuthenticationPrincipal User requester) {
        return reservationService.update(id, request, requester);
    }

    @PatchMapping("/{id}/cancel")
    public ReservationResponse cancel(@PathVariable Long id, @AuthenticationPrincipal User requester) {
        return reservationService.cancelOwnReservation(id, requester);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id, @AuthenticationPrincipal User requester) {
        reservationService.delete(id, requester);
    }
}
