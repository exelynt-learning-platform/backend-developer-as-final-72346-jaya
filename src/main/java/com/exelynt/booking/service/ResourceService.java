package com.exelynt.booking.service;

import com.exelynt.booking.dto.ResourceRequest;
import com.exelynt.booking.dto.ResourceResponse;
import com.exelynt.booking.entity.Resource;
import com.exelynt.booking.exception.NotFoundException;
import com.exelynt.booking.repository.ResourceRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ResourceService {
    private final ResourceRepository resourceRepository;

    public ResourceService(ResourceRepository resourceRepository) {
        this.resourceRepository = resourceRepository;
    }

    public Page<ResourceResponse> findAll(Pageable pageable) {
        return resourceRepository.findAll(pageable).map(this::toResponse);
    }

    public ResourceResponse findById(Long id) {
        return toResponse(getEntity(id));
    }

    @Transactional
    public ResourceResponse create(ResourceRequest request) {
        Resource resource = new Resource(
                request.name(),
                request.type(),
                request.description(),
                request.pricePerHour(),
                request.available() == null || request.available()
        );
        return toResponse(resourceRepository.save(resource));
    }

    @Transactional
    public ResourceResponse update(Long id, ResourceRequest request) {
        Resource resource = getEntity(id);
        resource.setName(request.name());
        resource.setType(request.type());
        resource.setDescription(request.description());
        resource.setPricePerHour(request.pricePerHour());
        resource.setAvailable(request.available() == null || request.available());
        return toResponse(resource);
    }

    @Transactional
    public void delete(Long id) {
        Resource resource = getEntity(id);
        resourceRepository.delete(resource);
    }

    public Resource getEntity(Long id) {
        return resourceRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Resource not found"));
    }

    public ResourceResponse toResponse(Resource resource) {
        return new ResourceResponse(
                resource.getId(),
                resource.getName(),
                resource.getType(),
                resource.getDescription(),
                resource.getPricePerHour(),
                resource.isAvailable()
        );
    }
}
