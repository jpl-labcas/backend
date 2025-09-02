package gov.nasa.jpl.labcas.data_access_api.jwt;

import java.util.concurrent.ConcurrentHashMap;
import java.util.UUID;
import java.util.logging.Logger;
import java.util.Date;


public class Sessions {
    private final static Logger LOG = Logger.getLogger(Sessions.class.getName());
    public static final Sessions INSTANCE = new Sessions();
    private Sessions() {}

    /** Known sessions.
     * 
     * 👉 NOTE: there's no way to expire old sessions unless we have some background thread or
     * something. Redis or Memcached would be a better solution.
     * 
     * I'm recommending putting in a weekly restart in cron to keep this fresh. If it kicks out
     * an active user, well, that's a bummer.
     */
    private static ConcurrentHashMap<String, Session> sessions = new ConcurrentHashMap<>();  // UUID → Session

    public Session startSession(String subject) {
        String sessionID = UUID.randomUUID().toString();
        Session session = new Session(sessionID, subject);
        LOG.info("💆 Starting session for " + subject + " with ID " + sessionID);
        sessions.put(sessionID, session);
        return session;
    }

    public void endSession(String sessionID) {
        // TODO: we might need an explicit /logout endpoint so the UI can force expiration of the session
        LOG.info("💆 Ending session for " + sessionID);
        sessions.remove(sessionID);
    }

    public boolean isSessionValid(String sessionID, String subject) {
        Session session = sessions.get(sessionID);
        if (session == null) {
            return false;
        }
        LOG.info("💆 Checking session for " + subject + " with ID " + sessionID);
        boolean isUserValid = session.getSubject().equals(subject);
        if (!isUserValid) return false;
        boolean isExpired = session.getExpiresAt().before(new Date());
        if (isExpired) {
            LOG.info("💆 Session for " + subject + " with ID " + sessionID + " has expired; removing it");
            sessions.remove(sessionID);
        }
        LOG.info("💆 Session for " + subject + " with ID " + sessionID
            + " hasn't expired (" + session.getExpiresAt() + ") and is valid");
        return true;
    }
}
