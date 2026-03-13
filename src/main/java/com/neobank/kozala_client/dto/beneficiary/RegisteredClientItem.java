package com.neobank.kozala_client.dto.beneficiary;

import lombok.*;

/**
 * Résumé d'un client inscrit pour l'affichage dans la page "contacts" (répertoire).
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RegisteredClientItem {

    private Long id;
    private String displayName;
    private String phone;
}
