
package org.xbib.elasticsearch.common.bytes;

import org.elasticsearch.ElasticsearchException;
import org.elasticsearch.common.lease.Releasables;
import org.elasticsearch.common.util.BigArrays;
import org.elasticsearch.common.util.ByteArray;

/**
 * An extension to {@link org.elasticsearch.common.bytes.PagedBytesReference} that requires releasing its content. This
 * class exists to make it explicit when a bytes reference needs to be released, and when not.
 */
public class ReleasablePagedBytesReference extends PagedBytesReference implements ReleasableBytesReference {

    public ReleasablePagedBytesReference(BigArrays bigarrays, ByteArray bytearray, int length) {
        super(bigarrays, bytearray, length);
    }

    public ReleasablePagedBytesReference(BigArrays bigarrays, ByteArray bytearray, int from, int length) {
        super(bigarrays, bytearray, from, length);
    }

    @Override
    public void close() throws ElasticsearchException {
        Releasables.close(bytearray);
    }
}
