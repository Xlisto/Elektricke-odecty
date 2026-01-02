package cz.xlisto.elektrodroid.utils;


/**
 * Jednorázový obal pro události (single-use event).
 * Umožňuje, aby byl obsah události vyzvednut pouze jednou.
 *
 * @param <T> typ obsahu události
 */
public class Event<T> {

    private final T content;
    private boolean handled = false;


    /**
     * Vytvoří novou událost s daným obsahem.
     *
     * @param content obsah události; může být {@code null}
     */
    public Event(T content) {
        this.content = content;
    }


    /**
     * Vrátí obsah události, pokud ještě nebyl vyřízen.
     * Metoda je synchronizovaná, aby bylo zřejmé, že kontrola a
     * nastavení příznaku {@code handled} proběhne atomicky v rámci jednoho vlákna.
     * <p>
     * Při prvním volání se vrátí obsah a interní příznak {@code handled} se nastaví na {@code true}.
     * Při dalším volání metoda vrátí {@code null}.
     *
     * @return obsah při prvním volání, jinak {@code null}
     */
    public synchronized T getContentIfNotHandled() {
        if (handled) return null;
        handled = true;
        return content;
    }

}
