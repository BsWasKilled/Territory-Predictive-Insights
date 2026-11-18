package dominio;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import excecao.DadosInvalidosException;

/**
 * Estação de monitoramento: um ponto do território que reúne um ou mais
 * sensores. Seu status depende de quantos sensores estão respondendo.
 */
public class EstacaoMonitoramento extends ComponenteTerritorial {

    private final List<Sensor> sensores = new ArrayList<>();

    public EstacaoMonitoramento(String id, String nome, String regiao) {
        super(id, nome, regiao);
    }

    public void adicionarSensor(Sensor sensor) {
        if (sensor == null) {
            throw new DadosInvalidosException("a estação " + getId() + " não pode receber um sensor vazio");
        }
        sensores.add(sensor);
    }

    /** Lista dos sensores, somente para leitura. */
    public List<Sensor> getSensores() {
        return Collections.unmodifiableList(sensores);
    }

    public int contarSensoresFuncionando() {
        int funcionando = 0;
        for (Sensor sensor : sensores) {
            if (sensor.verificarFuncionamento()) {
                funcionando++;
            }
        }
        return funcionando;
    }

    @Override
    public StatusComponente verificarStatus() {
        int funcionando = contarSensoresFuncionando();
        if (funcionando == 0) {
            setStatus(StatusComponente.INOPERANTE);
        } else if (funcionando == sensores.size()) {
            setStatus(StatusComponente.OPERACIONAL);
        } else {
            setStatus(StatusComponente.PARCIAL);
        }
        return getStatus();
    }
}
