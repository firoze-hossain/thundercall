package com.roze.thundercall.api.service;

import com.roze.thundercall.api.entity.TutorialStatus;
import com.roze.thundercall.api.entity.User;

import java.util.Optional;

public interface TutorialStatusService {
    TutorialStatus getOrCreateTutorialStatus(User user);

    boolean isTutorialCompleted(User user);

    TutorialStatus markStepComplete(User user, String stepId);

    Optional<TutorialStatus> findByUser(User user);
}
