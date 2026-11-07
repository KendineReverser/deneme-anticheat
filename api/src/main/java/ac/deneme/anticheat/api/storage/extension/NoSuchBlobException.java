package ac.deneme.anticheat.api.storage.extension;

import ac.deneme.anticheat.api.storage.model.BlobRef;
import org.jetbrains.annotations.ApiStatus;

import java.io.IOException;

@ApiStatus.Experimental
public class NoSuchBlobException extends IOException {

    public NoSuchBlobException(BlobRef ref) {
        super("blob not found: " + ref);
    }
}
