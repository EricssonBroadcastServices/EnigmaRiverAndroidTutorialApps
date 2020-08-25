package enigma.redbeemedia.com.downloads.trackui;

public abstract class ItemAdapter<T> {
    public final T object;

    public ItemAdapter(T object) {
        this.object = object;
    }

    @Override
    public String toString() {
        return getLabel(object);
    }

    protected abstract String getLabel(T obj);
}