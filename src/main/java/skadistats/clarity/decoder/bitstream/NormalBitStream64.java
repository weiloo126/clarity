package skadistats.clarity.decoder.bitstream;

import com.google.protobuf.ByteString;
import com.google.protobuf.ZeroCopy;
import skadistats.clarity.decoder.Util;

public class NormalBitStream64 extends BitStream {

    private final long[] data;

    protected NormalBitStream64(ByteString input) {
        len = input.size();
        data = new long[(len + 15)  >> 3];
        pos = 0;
        Util.byteCopy(ZeroCopy.extract(input), 0, data, 0, len);
        len = len * 8; // from now on size in bits
    }

    protected int peekBit(int pos) {
        return (int)((data[pos >> 6] >> (pos & 63)) & 1L);
    }

    @Override
    public int readUBitInt(int n) {
        assert n <= 32;
        int start = pos >> 6;
        int end = (pos + n - 1) >> 6;
        int s = pos & 63;
        pos += n;
        if (start == end) {
            return (int)((data[start] >>> s) & MASKS[n]);
        } else { // wrap around
            return (int)(((data[start] >>> s) | (data[end] << (64 - s))) & MASKS[n]);
        }
    }

    @Override
    public long readUBitLong(int n) {
        assert n <= 64;
        int start = pos >> 6;
        int end = (pos + n - 1) >> 6;
        int s = pos & 63;
        pos += n;
        if (start == end) {
            return (data[start] >>> s) & MASKS[n];
        } else { // wrap around
            return ((data[start] >>> s) | (data[end] << (64 - s))) & MASKS[n];
        }
    }

    @Override
    public byte[] readBitsAsByteArray(int n) {
        int nBytes = (n + 7) / 8;
        byte[] result = new byte[nBytes];
        if ((pos & 7) == 0) {
            Util.byteCopy(data, pos >> 3, result, 0, nBytes);
            pos += n;
            return result;
        }
        int i = 0;
        while (n > 7) {
            result[i++] = (byte) readUBitInt(8);
            n -= 8;
        }
        if (n != 0) {
            result[i] = (byte) readUBitInt(n);
        }
        return result;
    }

}