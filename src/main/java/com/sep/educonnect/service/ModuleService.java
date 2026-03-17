package com.sep.educonnect.service;

import com.sep.educonnect.dto.module.ModuleRequest;
import com.sep.educonnect.dto.module.ModuleResponse;
import com.sep.educonnect.entity.Module;
import com.sep.educonnect.entity.Syllabus;
import com.sep.educonnect.exception.AppException;
import com.sep.educonnect.exception.ErrorCode;
import com.sep.educonnect.mapper.ModuleMapper;
import com.sep.educonnect.repository.ModuleRepository;
import com.sep.educonnect.repository.SyllabusRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ModuleService {

    private final ModuleRepository moduleRepository;
    private final ModuleMapper moduleMapper;
    private final SyllabusRepository syllabusRepository;

    public Page<ModuleResponse> getModulesBySyllabus(Long syllabusId, int page, int size, String sortBy, String direction) {
        Sort.Direction sortDirection = direction.equalsIgnoreCase("desc") ? Sort.Direction.DESC : Sort.Direction.ASC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(sortDirection, sortBy));
        Page<Module> modulePage = moduleRepository.findBySyllabusIdOrderByOrderNumberAscWithPaging(syllabusId, pageable);
        return modulePage.map(moduleMapper::toResponse);
    }


    public Optional<Module> getById(Long moduleId) {
        return moduleRepository.findById(moduleId);
    }

    public ModuleResponse getResponseById(Long moduleId) {
        Module module = moduleRepository.findById(moduleId).orElseThrow(() -> new AppException(ErrorCode.MODULE_NOT_EXISTED));
        return moduleMapper.toResponse(module);
    }

    @Transactional
    public ModuleResponse create(ModuleRequest request) {
        // Validate and fetch the Syllabus entity
        Syllabus syllabus = syllabusRepository.findById(request.getSyllabusId())
                .orElseThrow(() -> new AppException(ErrorCode.SYLLABUS_NOT_EXISTED));

        Module module = moduleMapper.toEntity(request);

        // Set the Syllabus entity reference (this will populate syllabusId automatically)
        module.setSyllabus(syllabus);

        if (module.getOrderNumber() == null) {
            int nextOrder = moduleRepository.findBySyllabusIdOrderByOrderNumberAsc(request.getSyllabusId()).size() + 1;
            module.setOrderNumber(nextOrder);
        }
        Module saved = moduleRepository.save(module);

        return moduleMapper.toResponse(saved);
    }

    @Transactional
    public ModuleResponse update(Long id, ModuleRequest request) {
        Module existing = moduleRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.MODULE_NOT_EXISTED));

        // If syllabusId is being changed, validate and update the Syllabus entity reference
        if (request.getSyllabusId() != null && !request.getSyllabusId().equals(existing.getSyllabusId())) {
            Syllabus syllabus = syllabusRepository.findById(request.getSyllabusId())
                    .orElseThrow(() -> new AppException(ErrorCode.SYLLABUS_NOT_EXISTED));
            existing.setSyllabus(syllabus);
        }

        moduleMapper.updateEntity(existing, request);

        Module saved = moduleRepository.save(existing);
        return moduleMapper.toResponse(saved);
    }

    @Transactional
    public void delete(Long id) {
        Module module = moduleRepository.findById(id).orElseThrow(() -> new AppException(ErrorCode.MODULE_NOT_EXISTED));
        moduleRepository.delete(module);
    }

    @Transactional
    public void reorder(Long moduleId, Integer newOrderNumber) {
        Module module = moduleRepository.findById(moduleId).orElseThrow(() -> new AppException(ErrorCode.MODULE_NOT_EXISTED));
        module.setOrderNumber(newOrderNumber);
        moduleRepository.save(module);
    }
}
