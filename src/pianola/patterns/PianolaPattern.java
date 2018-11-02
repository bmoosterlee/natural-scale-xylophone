package pianola.patterns;

import frequency.Frequency;
import gui.buckets.Buckets;

import java.util.Set;

public interface PianolaPattern {

    Set<Frequency> playPattern(Buckets noteBuckets, Buckets harmonicsBuckets);

}
