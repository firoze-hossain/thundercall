package com.roze.thundercall.api.service;

import com.roze.thundercall.api.dto.OnlineUserResponse;
import com.roze.thundercall.api.entity.User;

import java.util.List;

public interface PresenceService {
    /** Marks the given user as online right now. Called by the desktop
     * client's periodic heartbeat while it's running and logged in. */
    void heartbeat(User user);

    /** Currently-online users RELATED to the viewer: teammates, plus
     * anyone connected through workspace sharing (people the viewer
     * shared with, people who shared with the viewer, and co-members on
     * the same shared workspaces). Never includes the viewer themself,
     * and never leaks unrelated accounts. */
    List<OnlineUserResponse> getOnlineRelatedUsers(User viewer);
}
