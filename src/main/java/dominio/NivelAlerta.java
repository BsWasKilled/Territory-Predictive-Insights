package dominio;

/**
 * Níveis de alerta, do mais brando ao mais grave. Cada nível traz uma
 * orientação: ela é uma sugestão para quem decide, não uma ordem do sistema.
 */
public enum NivelAlerta {

    ATENCAO("ATENÇÃO",
            "acompanhar as próximas leituras e conferir bocas de lobo e pontos baixos"),
    ALERTA("ALERTA",
            "avisar os moradores das áreas baixas e deixar as equipes de prontidão"),
    CRITICO("CRÍTICO",
            "acionar a Defesa Civil (199) e orientar a saída preventiva dos pontos vulneráveis");

    private final String rotulo;
    private final String orientacao;

    NivelAlerta(String rotulo, String orientacao) {
        this.rotulo = rotulo;
        this.orientacao = orientacao;
    }

    /** Nome do nível como aparece para o usuário, com acentos. */
    public String getRotulo() {
        return rotulo;
    }

    public String getOrientacao() {
        return orientacao;
    }
}
