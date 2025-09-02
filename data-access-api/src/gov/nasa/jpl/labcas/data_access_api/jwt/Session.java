package gov.nasa.jpl.labcas.data_access_api.jwt;

import java.util.Date;


public class Session {
    private String sessionID;
    private String subject;
    private Date createdAt;
    private Date expiresAt;

    public Session(String sessionID, String subject) {
        this.sessionID = sessionID;
        this.subject = subject;
        this.createdAt = new Date();
        this.expiresAt = new Date(createdAt.getTime() + Constants.EXPIRES_IN_SECONDS * 1000);
    }

    public String getSessionID() {
        return sessionID;
    }

    public String getSubject() {
        return subject;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public Date getExpiresAt() {
        return expiresAt;
    }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Session session = (Session) o;
        return sessionID.equals(session.sessionID);
    }
    
    public int hashCode() {
        return sessionID.hashCode();
    }

    public String toString() {
        return "Session{" + "sessionID='" + sessionID + '\'' + ", subject='" + subject + '\''
            + ", createdAt=" + createdAt + ", expiresAt=" + expiresAt + '}';
    }
}
