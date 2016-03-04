package edu.brown.cs.systems.tracing.aspects;

import edu.brown.cs.systems.baggage.Baggage;
import edu.brown.cs.systems.baggage.DetachedBaggage;
import edu.brown.cs.systems.tracing.aspects.Annotations.BaggageInheritanceDisabled;
import edu.brown.cs.systems.xtrace.reporting.XTraceReport;

/** Instruments all runnables to add the following logic: 1. When the runnable object is created, the baggage at that
 * point in time is saved 2. When the runnable is run, the baggage that was saved is now resumed 3. When the run method
 * finishes, the baggage is cleared */
public aspect Runnables {

    declare parents: (!@BaggageInheritanceDisabled Runnable)+ implements BaggageAdded;

    before(BaggageAdded r): this(r) && execution(Runnable+.new(..)) {
        XTraceReport.entering(thisJoinPointStaticPart);
        try {
            r.saveBaggage(Baggage.fork());
        } finally {
            XTraceReport.left(thisJoinPointStaticPart);
        }
    }
    
    void around(BaggageAdded r): this(r) && execution(void Runnable+.run(..)) {
        XTraceReport.entering(thisJoinPointStaticPart);
        DetachedBaggage previousBaggage = Baggage.swap(r.getSavedBaggage());
        XTraceReport.left(thisJoinPointStaticPart);
        
        try {
            proceed(r);
        } finally {
            XTraceReport.entering(thisJoinPointStaticPart);
            r.saveBaggage(Baggage.swap(previousBaggage));
            XTraceReport.left(thisJoinPointStaticPart);
        }
    }
    
    // If a runnable is a shutdown hook, cancel it
    before(BaggageAdded r): call(* *.addShutdownHook(Runnable+,..)) && args(r,..) {
        r.discardSavedBaggage();
    }

}