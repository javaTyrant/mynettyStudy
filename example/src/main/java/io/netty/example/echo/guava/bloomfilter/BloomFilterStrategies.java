package io.netty.example.echo.guava.bloomfilter;

import com.google.common.hash.Funnel;
import com.google.common.hash.Hashing;
import com.google.common.math.LongMath;
import com.google.common.primitives.Ints;
import com.google.common.primitives.Longs;

import java.math.RoundingMode;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicLongArray;

import static com.google.common.base.Preconditions.checkArgument;

/**
 * @author lufengxiang
 * @since 2021/5/17
 **/
public enum BloomFilterStrategies implements MyBloomFilter.Strategy {
    MURMUR128_MITZ_32() {
        @Override
        public <T> boolean put(
                T object, Funnel<? super T> funnel, int numHashFunctions, BloomFilterStrategies.LockFreeBitArray bits) {
            long bitSize = bits.bitSize();
            long hash64 = Hashing.murmur3_128().hashObject(object, funnel).asLong();
            int hash1 = (int) hash64;
            int hash2 = (int) (hash64 >>> 32);

            boolean bitsChanged = false;
            for (int i = 1; i <= numHashFunctions; i++) {
                int combinedHash = hash1 + (i * hash2);
                // Flip all the bits if it's negative (guaranteed positive number)
                if (combinedHash < 0) {
                    combinedHash = ~combinedHash;
                }
                bitsChanged |= bits.set(combinedHash % bitSize);
            }
            return bitsChanged;
        }

        @Override
        public <T> boolean mightContain(
                T object, Funnel<? super T> funnel, int numHashFunctions, BloomFilterStrategies.LockFreeBitArray bits) {
            long bitSize = bits.bitSize();
            long hash64 = Hashing.murmur3_128().hashObject(object, funnel).asLong();
            int hash1 = (int) hash64;
            int hash2 = (int) (hash64 >>> 32);

            for (int i = 1; i <= numHashFunctions; i++) {
                int combinedHash = hash1 + (i * hash2);
                // Flip all the bits if it's negative (guaranteed positive number)
                if (combinedHash < 0) {
                    combinedHash = ~combinedHash;
                }
                if (!bits.get(combinedHash % bitSize)) {
                    return false;
                }
            }
            return true;
        }
    },

    MURMUR128_MITZ_64() {
        @Override
        public <T> boolean put(
                T object, Funnel<? super T> funnel, int numHashFunctions, BloomFilterStrategies.LockFreeBitArray bits) {
            long bitSize = bits.bitSize();
            byte[] bytes = null;//Hashing.murmur3_128().hashObject(object, funnel).getBytesInternal();
            long hash1 = lowerEight(bytes);
            long hash2 = upperEight(bytes);

            boolean bitsChanged = false;
            long combinedHash = hash1;
            for (int i = 0; i < numHashFunctions; i++) {
                // Make the combined hash positive and indexable
                bitsChanged |= bits.set((combinedHash & Long.MAX_VALUE) % bitSize);
                combinedHash += hash2;
            }
            return bitsChanged;
        }

        @Override
        public <T> boolean mightContain(
                T object, Funnel<? super T> funnel, int numHashFunctions, BloomFilterStrategies.LockFreeBitArray bits) {
            long bitSize = bits.bitSize();
            byte[] bytes = null;//Hashing.murmur3_128().hashObject(object, funnel).getBytesInternal();
            long hash1 = lowerEight(bytes);
            long hash2 = upperEight(bytes);

            long combinedHash = hash1;
            for (int i = 0; i < numHashFunctions; i++) {
                // Make the combined hash positive and indexable
                if (!bits.get((combinedHash & Long.MAX_VALUE) % bitSize)) {
                    return false;
                }
                combinedHash += hash2;
            }
            return true;
        }

        private /* static */ long lowerEight(byte[] bytes) {
            return Longs.fromBytes(
                    bytes[7], bytes[6], bytes[5], bytes[4], bytes[3], bytes[2], bytes[1], bytes[0]);
        }

        private /* static */ long upperEight(byte[] bytes) {
            return Longs.fromBytes(
                    bytes[15], bytes[14], bytes[13], bytes[12], bytes[11], bytes[10], bytes[9], bytes[8]);
        }
    };

    static final class LockFreeBitArray {
        //
        private static final int LONG_ADDRESSABLE_BITS = 6;
        //
        final AtomicLongArray data;
        //
        private final LongAddable bitCount;

        LockFreeBitArray(long bits) {
            checkArgument(bits > 0, "data length is zero!");
            // Avoid delegating to this(long[]), since AtomicLongArray(long[]) will clone its input and
            // thus double memory usage.
            this.data =
                    new AtomicLongArray(Ints.checkedCast(LongMath.divide(bits, 64, RoundingMode.CEILING)));
            this.bitCount = LongAddables.create();
        }

        // Used by serialization
        LockFreeBitArray(long[] data) {
            checkArgument(data.length > 0, "data length is zero!");
            this.data = new AtomicLongArray(data);
            this.bitCount = LongAddables.create();
            long bitCount = 0;
            for (long value : data) {
                bitCount += Long.bitCount(value);
            }
            this.bitCount.add(bitCount);
        }

        boolean set(long bitIndex) {
            if (get(bitIndex)) {
                return false;
            }

            int longIndex = (int) (bitIndex >>> LONG_ADDRESSABLE_BITS);
            long mask = 1L << bitIndex; // only cares about low 6 bits of bitIndex

            long oldValue;
            long newValue;
            do {
                oldValue = data.get(longIndex);
                newValue = oldValue | mask;
                if (oldValue == newValue) {
                    return false;
                }
            } while (!data.compareAndSet(longIndex, oldValue, newValue));

            // We turned the bit on, so increment bitCount.
            bitCount.increment();
            return true;
        }

        boolean get(long bitIndex) {
            return (data.get((int) (bitIndex >>> LONG_ADDRESSABLE_BITS)) & (1L << bitIndex)) != 0;
        }

        public static long[] toPlainArray(AtomicLongArray atomicLongArray) {
            long[] array = new long[atomicLongArray.length()];
            for (int i = 0; i < array.length; ++i) {
                array[i] = atomicLongArray.get(i);
            }
            return array;
        }

        long bitSize() {
            return (long) data.length() * Long.SIZE;
        }

        long bitCount() {
            return bitCount.sum();
        }

        BloomFilterStrategies.LockFreeBitArray copy() {
            return new BloomFilterStrategies.LockFreeBitArray(toPlainArray(data));
        }

        void putAll(BloomFilterStrategies.LockFreeBitArray other) {
            checkArgument(
                    data.length() == other.data.length(),
                    "BitArrays must be of equal length (%s != %s)",
                    data.length(),
                    other.data.length());
            for (int i = 0; i < data.length(); i++) {
                long otherLong = other.data.get(i);

                long ourLongOld;
                long ourLongNew;
                boolean changedAnyBits = true;
                do {
                    ourLongOld = data.get(i);
                    ourLongNew = ourLongOld | otherLong;
                    if (ourLongOld == ourLongNew) {
                        changedAnyBits = false;
                        break;
                    }
                } while (!data.compareAndSet(i, ourLongOld, ourLongNew));

                if (changedAnyBits) {
                    int bitsAdded = Long.bitCount(ourLongNew) - Long.bitCount(ourLongOld);
                    bitCount.add(bitsAdded);
                }
            }
        }

        @Override
        public boolean equals(Object o) {
            if (o instanceof BloomFilterStrategies.LockFreeBitArray) {
                BloomFilterStrategies.LockFreeBitArray lockFreeBitArray = (BloomFilterStrategies.LockFreeBitArray) o;
                // TODO(lowasser): avoid allocation here
                return Arrays.equals(toPlainArray(data), toPlainArray(lockFreeBitArray.data));
            }
            return false;
        }

        @Override
        public int hashCode() {
            // TODO(lowasser): avoid allocation here
            return Arrays.hashCode(toPlainArray(data));
        }
    }
}
