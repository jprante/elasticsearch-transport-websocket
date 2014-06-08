
package org.xbib.elasticsearch.common.io.stream;

import org.xbib.elasticsearch.common.bytes.ReleasableBytesReference;
import org.xbib.elasticsearch.common.bytes.ReleasablePagedBytesReference;
import org.xbib.elasticsearch.common.io.ReleasableBytesStream;
import org.elasticsearch.common.util.BigArrays;

public class ReleasableBytesStreamOutput extends BytesStreamOutput implements ReleasableBytesStream {

    public ReleasableBytesStreamOutput(BigArrays bigarrays) {
        super(BigArrays.PAGE_SIZE_IN_BYTES, bigarrays);
    }

    public ReleasableBytesStreamOutput(int expectedSize, BigArrays bigarrays) {
        super(expectedSize, bigarrays);
    }

    @Override
    public ReleasableBytesReference ourBytes() {
        return new ReleasablePagedBytesReference(bigarrays, bytes, count);
    }
}
