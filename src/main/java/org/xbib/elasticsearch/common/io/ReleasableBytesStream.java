
package org.xbib.elasticsearch.common.io;

import org.xbib.elasticsearch.common.bytes.ReleasableBytesReference;
import org.elasticsearch.common.io.BytesStream;

/**
 * A bytes stream that requires its bytes to be released once no longer used.
 */
public interface ReleasableBytesStream extends BytesStream {

    ReleasableBytesReference ourBytes();
}
