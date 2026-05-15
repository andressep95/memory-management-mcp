package com.cloudcentinel.memory_management_mcp.domain.session.entity;

import com.cloudcentinel.memory_management_mcp.domain.project.valueobject.ProjectId;
import com.cloudcentinel.memory_management_mcp.domain.session.event.SessionClosed;
import com.cloudcentinel.memory_management_mcp.domain.session.event.SessionStarted;
import com.cloudcentinel.memory_management_mcp.domain.session.event.SkillQueried;
import com.cloudcentinel.memory_management_mcp.domain.session.valueobject.SessionId;
import com.cloudcentinel.memory_management_mcp.domain.skill.valueobject.SkillId;
import com.cloudcentinel.memory_management_mcp.domain.user.valueobject.UserId;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Session {

    private final SessionId id;
    private final UserId    userId;
    private final ProjectId projectId;
    private final Instant   startedAt;
    private Instant         lastActivity;
    private Instant         closedAt;

    private final List<SkillUsageRecord> usageRecords = new ArrayList<>();
    private final List<Object>           domainEvents  = new ArrayList<>();

    private Session(SessionId id, UserId userId, ProjectId projectId, Instant startedAt) {
        this.id           = id;
        this.userId       = userId;
        this.projectId    = projectId;
        this.startedAt    = startedAt;
        this.lastActivity = startedAt;
    }

    public static Session start(UserId userId, ProjectId projectId) {
        Session session = new Session(SessionId.generate(), userId, projectId, Instant.now());
        session.domainEvents.add(new SessionStarted(session.id, session.userId, session.projectId));
        return session;
    }

    public static Session reconstitute(SessionId id, UserId userId, ProjectId projectId,
                                       Instant startedAt, Instant lastActivity, Instant closedAt,
                                       List<SkillUsageRecord> records) {
        Session session = new Session(id, userId, projectId, startedAt);
        session.lastActivity = lastActivity;
        session.closedAt     = closedAt;
        session.usageRecords.addAll(records);
        return session;
    }

    public void recordSkillUsage(SkillId skillId, String queryText) {
        if (!isActive()) throw new IllegalStateException("Cannot record usage on a closed session");
        usageRecords.add(new SkillUsageRecord(skillId, queryText));
        this.lastActivity = Instant.now();
        domainEvents.add(new SkillQueried(id, skillId, queryText));
    }

    public void close() {
        if (!isActive()) throw new IllegalStateException("Session is already closed");
        this.closedAt = Instant.now();
        domainEvents.add(new SessionClosed(id));
    }

    public boolean isActive() { return closedAt == null; }

    public List<Object> pullEvents() {
        List<Object> events = new ArrayList<>(domainEvents);
        domainEvents.clear();
        return Collections.unmodifiableList(events);
    }

    public SessionId              id()           { return id; }
    public UserId                 userId()       { return userId; }
    public ProjectId              projectId()    { return projectId; }
    public Instant                startedAt()    { return startedAt; }
    public Instant                lastActivity() { return lastActivity; }
    public Instant                closedAt()     { return closedAt; }
    public List<SkillUsageRecord> usageRecords() { return Collections.unmodifiableList(usageRecords); }
}
