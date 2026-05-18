package com.cloudcentinel.memory_management_mcp.domain.session.entity;

import com.cloudcentinel.memory_management_mcp.domain.session.event.SessionClosed;
import com.cloudcentinel.memory_management_mcp.domain.session.event.SessionStarted;
import com.cloudcentinel.memory_management_mcp.domain.session.event.SkillQueried;
import com.cloudcentinel.memory_management_mcp.domain.session.valueobject.SessionId;
import com.cloudcentinel.memory_management_mcp.domain.skill.valueobject.SkillId;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Session {

    private final SessionId id;
    private final String    gitUsername;
    private final String    projectId;
    private final String    agent;
    private final Instant   startedAt;
    private Instant         lastActivity;
    private Instant         closedAt;

    private final List<SkillUsageRecord> usageRecords = new ArrayList<>();
    private final List<Object>           domainEvents  = new ArrayList<>();

    private Session(SessionId id, String gitUsername, String projectId, String agent, Instant startedAt) {
        this.id           = id;
        this.gitUsername  = gitUsername;
        this.projectId    = projectId;
        this.agent        = agent;
        this.startedAt    = startedAt;
        this.lastActivity = startedAt;
    }

    public static Session start(String gitUsername, String projectId, String agent) {
        Session session = new Session(SessionId.generate(), gitUsername, projectId, agent, Instant.now());
        session.domainEvents.add(new SessionStarted(session.id, session.gitUsername, session.projectId));
        return session;
    }

    public static Session reconstitute(SessionId id, String gitUsername, String projectId,
                                       String agent, Instant startedAt, Instant lastActivity,
                                       Instant closedAt, List<SkillUsageRecord> records) {
        Session session = new Session(id, gitUsername, projectId, agent, startedAt);
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
    public String                 gitUsername()  { return gitUsername; }
    public String                 projectId()    { return projectId; }
    public String                 agent()        { return agent; }
    public Instant                startedAt()    { return startedAt; }
    public Instant                lastActivity() { return lastActivity; }
    public Instant                closedAt()     { return closedAt; }
    public List<SkillUsageRecord> usageRecords() { return Collections.unmodifiableList(usageRecords); }
}
