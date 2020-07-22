package enigma.redbeemedia.com.downloads.user;

import com.redbeemedia.enigma.core.session.ISession;

import java.util.List;

public class UserData {
    private final ISession session;
    private final List<String> availabilityKeys;

    public UserData(ISession session, List<String> availabilityKeys) {
        this.session = session;
        this.availabilityKeys = availabilityKeys;
    }

    public ISession getSession() {
        return session;
    }

    public List<String> getAvailabilityKeys() {
        return availabilityKeys;
    }
}
