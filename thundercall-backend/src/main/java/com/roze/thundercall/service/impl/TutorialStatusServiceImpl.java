package com.roze.thundercall.service.impl;

import com.roze.thundercall.entity.TutorialStatus;
import com.roze.thundercall.entity.User;
import com.roze.thundercall.repository.TutorialStatusRepository;
import com.roze.thundercall.service.TutorialStatusService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class TutorialStatusServiceImpl implements TutorialStatusService {
    private final TutorialStatusRepository tutorialStatusRepository;

    @Override
    @Transactional
    public TutorialStatus getOrCreateTutorialStatus(User user) {
        return tutorialStatusRepository.findByUser(user).orElseGet(() -> createInitialTutorialStatus(user));
    }

    @Override
    public boolean isTutorialCompleted(User user) {
        return tutorialStatusRepository.findByUser(user).map(TutorialStatus::isCompleted).orElse(false);
    }

    private TutorialStatus createInitialTutorialStatus(User user) {
        TutorialStatus status = TutorialStatus.builder()
                .user(user)
                .completedSteps(new ArrayList<>())
                .currentStep("Welcome")
                .completed(false)
                .build();
        return tutorialStatusRepository.save(status);
    }

    @Override
    @Transactional
    public TutorialStatus markStepComplete(User user, String stepId) {
        TutorialStatus status = getOrCreateTutorialStatus(user);
        if (!status.getCompletedSteps().contains(stepId)) {
            status.getCompletedSteps().add(stepId);
            if (status.getCompletedSteps().size() >= 6) {
                status.setCompleted(true);
            }
            return tutorialStatusRepository.save(status);
        }
        return status;
    }

    @Override
    public Optional<TutorialStatus> findByUser(User user) {
        return tutorialStatusRepository.findByUser(user);
    }
}
