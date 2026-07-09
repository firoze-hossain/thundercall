package com.roze.thundercall.service;

import com.roze.thundercall.entity.TutorialStatus;
import com.roze.thundercall.entity.User;

import java.util.Optional;

public interface TutorialStatusService {
    TutorialStatus getOrCreateTutorialStatus(User user);

    boolean isTutorialCompleted(User user);

    TutorialStatus markStepComplete(User user, String stepId);

    Optional<TutorialStatus> findByUser(User user);
}
