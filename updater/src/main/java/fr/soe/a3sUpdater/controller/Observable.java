package fr.soe.a3sUpdater.controller;

public interface Observable {
    void addObservateur(Observateur observateur);
    void updateObservateur();
    void delObservateur();
}
