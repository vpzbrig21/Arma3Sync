package fr.soe.a3s.domain.configration;

import java.io.Serializable;

/**
 * @deprecated This no-op stub is preserved for backward compatibility with
 *             existing serialized configurations.
 */
@Deprecated
public class AiAOptions implements Serializable {

        /**
         *
         */
        private static final long serialVersionUID = -2248371007390169026L;

        // Legacy fields retained to satisfy deserialization of older configs
        private String arma2Path;
        private String armaPath;
        private String tohPath;
        private String arma2OAPath;
        private String allinArmaPath;
}
