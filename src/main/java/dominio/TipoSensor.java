package dominio;

/**
 * Grandezas que o sistema acompanha no recorte "clima local": chuva intensa e
 * risco de alagamento. Cada tipo conhece a própria unidade e a faixa de valores
 * que um sensor de verdade consegue medir; fora dela, a leitura é inválida.
 */
public enum TipoSensor {

    CHUVA("chuva", "mm/h", 0.0, 150.0),
    NIVEL_RIO("nível do córrego", "cm", 0.0, 500.0),
    DRENAGEM("ocupação da galeria pluvial", "%", 0.0, 100.0);

    private final String descricao;
    private final String unidade;
    private final double minimo;
    private final double maximo;

    TipoSensor(String descricao, String unidade, double minimo, double maximo) {
        this.descricao = descricao;
        this.unidade = unidade;
        this.minimo = minimo;
        this.maximo = maximo;
    }

    public String getDescricao() {
        return descricao;
    }

    public String getUnidade() {
        return unidade;
    }

    public double getMinimo() {
        return minimo;
    }

    public double getMaximo() {
        return maximo;
    }

    /** Valor acompanhado da unidade, pronto para exibir (ex.: 12,5 mm/h). */
    public String formatar(double valor) {
        return Formato.numero(valor) + " " + unidade;
    }
}
