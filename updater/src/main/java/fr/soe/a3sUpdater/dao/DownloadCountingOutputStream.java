package fr.soe.a3sUpdater.dao;

import fr.soe.a3sUpdater.controller.Observable;
import fr.soe.a3sUpdater.controller.Observateur;

import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/** Counts downloaded bytes and preserves the observer role of the original class. */
public final class DownloadCountingOutputStream extends FilterOutputStream implements Observable {
    private final List<Observateur> observers = new CopyOnWriteArrayList<>();
    private long count;

    public DownloadCountingOutputStream(OutputStream outputStream) {
        super(outputStream);
    }

    @Override
    public void write(int value) throws IOException {
        super.write(value);
        count++;
        updateObservateur();
    }

    @Override
    public void write(byte[] bytes, int offset, int length) throws IOException {
        super.write(bytes, offset, length);
        count += length;
        updateObservateur();
    }

    public long getLongCount() { return count; }

    @Override
    public void addObservateur(Observateur observateur) { observers.add(observateur); }

    @Override
    public void delObservateur() { observers.clear(); }

    @Override
    public void updateObservateur() {
        int value = count > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) count;
        observers.forEach(observer -> observer.update(value));
    }
}
