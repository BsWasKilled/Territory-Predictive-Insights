package dominio;

import java.util.Optional;

import excecao.DadosInvalidosException;

/**
 * Os três limites de uma grandeza: a partir de que valor a situação passa a
 * ser de ATENÇÃO, de ALERTA e CRÍTICA. É imutável; para mudar um limiar cria-se
 * outro objeto, que passa de novo pela validação.
 */
public class Limiares {

    private final double atencao;
    private final double alerta;
    private final double critico;

    public Limiares(double atencao, double alerta, double critico) {
        if (naoENumero(atencao) || naoENumero(alerta) || naoENumero(critico)) {
            throw new DadosInvalidosException("limiar inválido: os três limites precisam ser números");
        }
        if (atencao <= 0) {
            throw new DadosInvalidosException(
                    "limiar inválido: o limite de atenção deve ser maior que zero (informado: "
                            + Formato.numero(atencao) + ")");
        }
        if (atencao >= alerta || alerta >= critico) {
            throw new DadosInvalidosException(
                    "limiar inválido: os limites devem crescer na ordem atenção < alerta < crítico (informado: "
                            + Formato.numero(atencao) + " / " + Formato.numero(alerta)
                            + " / " + Formato.numero(critico) + ")");
        }
        this.atencao = atencao;
        this.alerta = alerta;
        this.critico = critico;
    }

    private static boolean naoENumero(double valor) {
        return Double.isNaN(valor) || Double.isInfinite(valor);
    }

    /**
     * Compara um valor com os limites. Devolve o nível mais grave atingido, ou
     * vazio quando o valor está abaixo do limite de atenção (situação normal).
     */
    public Optional<NivelAlerta> classificar(double valor) {
        if (valor >= critico) {
            return Optional.of(NivelAlerta.CRITICO);
        }
        if (valor >= alerta) {
            return Optional.of(NivelAlerta.ALERTA);
        }
        if (valor >= atencao) {
            return Optional.of(NivelAlerta.ATENCAO);
        }
        return Optional.empty();
    }

    /** Limite que precisa ser atingido para a situação chegar ao nível informado. */
    public double getLimite(NivelAlerta nivel) {
        switch (nivel) {
            case ATENCAO:
                return atencao;
            case ALERTA:
                return alerta;
            default:
                return critico;
        }
    }

    public double getAtencao() {
        return atencao;
    }

    public double getAlerta() {
        return alerta;
    }

    public double getCritico() {
        return critico;
    }
}
