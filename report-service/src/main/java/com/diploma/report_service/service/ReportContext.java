package com.diploma.report_service.service;

import com.diploma.report_service.client.ReviewClient;
import com.diploma.report_service.client.SessionAccessClient;
import com.diploma.report_service.client.SnapshotClient;
import com.diploma.report_service.client.UserClient;

public record ReportContext(
        SessionAccessClient.SessionInfo session,
        ReviewClient.ReviewInfo review,
        SnapshotClient.SnapshotInfo snapshot,
        UserClient.UserInfo interviewer,
        UserClient.UserInfo candidate
) {}
