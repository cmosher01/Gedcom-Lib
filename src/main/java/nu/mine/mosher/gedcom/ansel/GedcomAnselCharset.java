package nu.mine.mosher.gedcom.ansel;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.CoderResult;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static nu.mine.mosher.gedcom.ansel.AnselCharacterMap.map;

/**
 * @author Chris Mosher
 */
public class GedcomAnselCharset extends Charset {
    static final String name = "x-gedcom-ansel";

    /**
     *
     */
    public GedcomAnselCharset() {
        this(name, null);
    }

    /**
     * @param canonicalName
     * @param aliases
     */
    public GedcomAnselCharset(final String canonicalName, final String[] aliases) {
        super(canonicalName, aliases);
    }

    @Override
    public boolean contains(final Charset cs) {
        return cs instanceof GedcomAnselCharset;
    }

    @Override
    public CharsetDecoder newDecoder() {
        return new CharsetDecoder(this, 1, 1) {
            private final List<Integer> listCombining = new ArrayList<Integer>();
            private boolean combining = false;

            @Override
            protected CoderResult decodeLoop(final ByteBuffer in, final CharBuffer out) {
                if (!this.combining) {
                    while (out.position() < out.limit() && this.listCombining.size() > 0) {
                        final int cint = this.listCombining.remove(0);
                        out.put((char) cint);
                    }
                }
                while (in.hasRemaining() && out.position() < out.limit()) {
                    final int bint = getChar(in);

                    char c;
                    this.combining = false;
                    if (map.containsKey(bint)) {
                        final int mappedUni = map.get(bint);
                        c = (char) mappedUni;

                        if (c != 0 && bint >= 0xe0) // ANSEL standard: any char E0-FF is combining (with *following* char)
                        {
                            this.listCombining.add((int) c);
                            this.combining = true;
                        }
                    } else {
                        c = (char) bint;
                        c &= 0xff;
                    }
                    if (!this.combining) {
                        if (c != 0) {
                            out.put(c);
                        }
                        while (out.position() < out.limit() && this.listCombining.size() > 0) {
                            final int cint = this.listCombining.remove(0);
                            out.put((char) cint);
                        }
                    }
                }
                return in.hasRemaining() ? CoderResult.OVERFLOW : CoderResult.UNDERFLOW;
            }

            private int getChar(final ByteBuffer in) {
                int bint = in.get();
                bint &= 0xff;
                return bint;
            }
        };
    }

    @Override
    public CharsetEncoder newEncoder() {
        throw new UnsupportedOperationException("encoding to ANSEL is not supported.");
//        return new CharsetEncoder(this,1,1)
//        {
//        	// TODO fix ansel encoding (needs to reverse combining chars)
//            protected CoderResult encodeLoop(CharBuffer in, ByteBuffer out)
//            {
//				Map map = GedcomAnselTable.getEncoder();
//				while (in.hasRemaining())
//				{
//					Character c = new Character(in.get());
//					byte b;
//
//					if (map.containsKey(c))
//					{
//						b = ((Byte)map.get(c)).byteValue();
//					}
//					else
//					{
//						char cn = c.charValue();
//						if (cn >= 0x100)
//							return CoderResult.unmappableForLength(1);
//						b = (byte)cn;
//					}
//
//					out.put(b);
//				}
//				return in.hasRemaining() ? CoderResult.OVERFLOW : CoderResult.UNDERFLOW;
//            }
//        };
    }
}
