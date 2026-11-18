# Territory Predictive Insights

Global Solution 2º/2026 de POO (FIAP, turma de fevereiro). Tema: CitizenTech.

Sistema de console que monitora um território sujeito a alagamento. Ele lê sensores simulados, grava cada medição em banco de dados, calcula previsões simples sobre o histórico gravado e emite alertas explicados quando a previsão atinge um limiar.

**Recorte escolhido:** clima local, chuva intensa e risco de alagamento em pontos vulneráveis.

## Como rodar

### O que precisa estar instalado

- JDK 17 ou mais novo (`java -version`)
- Maven 3.8 ou mais novo (`mvn -version`), com internet na primeira execução para baixar as dependências

Não é preciso instalar banco de dados: o H2 vem embarcado e cria as tabelas sozinho.

### Pelo terminal

Na pasta do projeto (onde está o `pom.xml`):

```
mvn clean package
java -jar target/territory-predictive-insights-fev.jar
```

O `mvn clean package` compila, roda os testes e gera o `.jar` já com o H2 dentro. Para rodar sem gerar o `.jar`:

```
mvn compile exec:java
```

### Pela IDE (IntelliJ ou Eclipse)

Abra a pasta como projeto Maven, espere as dependências baixarem e execute a classe `SistemaMonitoramento` (ela fica direto em `src/main/java`).

### Testes

```
mvn test
```

São 116 testes. Nenhum deles depende de digitação nem grava arquivo de banco.

### Banco de dados

| Item | Valor |
|---|---|
| Banco | H2 embarcado, acessado por JDBC puro |
| Onde fica | arquivo `dados/territorio.mv.db`, criado na primeira execução na pasta de onde o programa foi iniciado |
| Usuário e senha | `sa`, sem senha |
| Tabelas | `medicao` e `alerta`, criadas por `ConexaoBanco.prepararBanco()` com `CREATE TABLE IF NOT EXISTS` |

O banco é gravado em arquivo porque o enunciado pede que os dados sobrevivam ao reiniciar o programa. Para começar do zero, feche o programa e apague a pasta `dados`.

Para uma execução que não deixa nada gravado, use o H2 em memória:

```
java "-Dbanco.url=jdbc:h2:mem:territorio;DB_CLOSE_DELAY=-1" -jar target/territory-predictive-insights-fev.jar
```

O programa só pode estar aberto em uma janela por vez, porque o H2 embarcado trava o arquivo. Se uma segunda janela for aberta, ela avisa e encerra.

### Código de acesso

Alterar limiares exige o código de acesso `FIAP2026`.

## Roteiro de demonstração (3 minutos)

1. Opção **1**, digite `12`: são 12 rodadas de leitura nos três sensores. Aparecem leituras gravadas, sensores que não responderam (`FonteIndisponivelException`) e leituras descartadas por valor impossível (`DadosInvalidosException`).
2. Opção **2**: as duas estratégias de previsão rodam sobre o histórico do banco e os alertas são emitidos, cada um com sensor, valor previsto e limite atingido.
3. Opção **2** de novo: nenhum alerta é repetido, porque não houve leitura nova.
4. Opção **3**: histórico de um sensor e lista de alertas, com opção de exportar em CSV (pasta `exportacoes`).
5. Opção **4**, item **6**: tente alterar um limiar com código errado, depois com `FIAP2026` e valores fora de ordem, depois com valores válidos.
6. Opção **5**: status completo.
7. Opção **0** e abra o programa de novo: a abertura mostra quantas medições e alertas vieram das execuções anteriores.

Com o banco vazio ou com menos de 5 leituras de um sensor, a opção 2 responde "sem previsão" em vez de arriscar um número.

## Público beneficiado

**Quem sofre o problema:** moradores de áreas baixas próximas a córregos, onde a rua alaga em poucas dezenas de minutos de chuva forte. O território de exemplo é a "Comunidade Córrego Azul", descrita só por região e equipamento, sem dado de nenhuma pessoa.

**Quem usa o sistema:** a associação de moradores ou o núcleo comunitário de defesa civil, isto é, quem precisa decidir antes da água subir.

**Qual decisão o sistema apoia:** o que fazer nas próximas horas. Cada nível de alerta traz uma sugestão:

| Nível | Sugestão |
|---|---|
| ATENÇÃO | acompanhar as próximas leituras e conferir bocas de lobo e pontos baixos |
| ALERTA | avisar os moradores das áreas baixas e deixar as equipes de prontidão |
| CRÍTICO | acionar a Defesa Civil (199) e orientar a saída preventiva dos pontos vulneráveis |

A decisão continua sendo da pessoa. O sistema informa e explica.

## Como funciona

### Sensores

Três sensores simulados, em duas estações:

| Sensor | Classe | Grandeza | Faixa válida | Limiares (atenção / alerta / crítico) |
|---|---|---|---|---|
| `PLUV-01` | `SensorChuva` | chuva, em mm/h | 0 a 150 | 10 / 25 / 50 |
| `NIVEL-01` | `SensorNivelRio` | nível do córrego, em cm | 0 a 500 | 150 / 220 / 300 |
| `GAL-01` | `SensorDrenagem` | ocupação da galeria pluvial, em % | 0 a 100 | 60 / 80 / 95 |

Os valores são sorteados com `Random` em torno de uma "tempestade" que dura 16 leituras: começa calma, chega ao pico e volta a acalmar. O córrego e a galeria reagem com atraso em relação à chuva. Em cada leitura o sensor tem 8% de chance de não responder e 4% de chance de devolver um valor impossível (chuva negativa, por exemplo). Esses dois defeitos existem de propósito, para as exceções aparecerem no uso normal.

### Previsões

As duas estratégias leem o histórico do banco, nunca a memória do programa.

- **Média móvel** (`PrevisaoMediaMovel`): média das últimas 5 leituras. Suaviza oscilações, mas demora a perceber mudança brusca.
- **Comparação com o período anterior** (`PrevisaoComparacao`): compara a média das 3 últimas leituras com a das 3 anteriores e repete essa variação. Percebe tendência de subida ou queda, mas precisa de 6 leituras.

Na mesma tempestade elas costumam discordar: com o córrego subindo, a média móvel pode dar ALERTA enquanto a comparação já dá CRÍTICO. Mostrar as duas deixa essa diferença visível.

### Alertas

Cada previsão é comparada com os limiares do tipo de sensor. Atingir o limite já conta. O alerta gravado guarda sensor, estratégia, valor previsto, limite atingido e a leitura mais recente em que a previsão se baseou. Se a mesma previsão for rodada de novo sem leitura nova, o alerta não é emitido outra vez.

## Onde está cada requisito

| Requisito do enunciado | Onde está |
|---|---|
| `ComponenteTerritorial` abstrata com `verificarStatus()` e filha concreta | `dominio/ComponenteTerritorial`, `dominio/EstacaoMonitoramento` |
| Interface `Sensor` com 3 implementações | `dominio/Sensor`, `SensorChuva`, `SensorNivelRio`, `SensorDrenagem` (parte comum em `SensorSimulado`) |
| `DadosTerritorio` com atributos privados e código de acesso | `dominio/DadosTerritorio` |
| `EstrategiaPrevisao` com 2 filhas sobrescrevendo `prever()` e usando `super` | `dominio/EstrategiaPrevisao`, `PrevisaoMediaMovel`, `PrevisaoComparacao` |
| Escolha da estratégia sem `if tipo ==` nem `instanceof` | `servico/FabricaPrevisao` (mapa nome → receita) |
| `DadosInvalidosException` e `FonteIndisponivelException` | pacote `excecao`; lançadas em `Medicao`, `Limiares`, `DadosTerritorio`, `SensorSimulado`, `ServicoMonitoramento` |
| Captura no menu com mensagem clara, sem `catch` vazio | `SistemaMonitoramento.executarProtegido` e `verificarSensores` |
| `MedicaoDAO` e `AlertaDAO` com `PreparedStatement` e try-with-resources | pacote `persistencia` |
| Histórico por sensor e alertas emitidos no menu | opção 3 |
| Padrões criacionais | `FabricaPrevisao` (Factory), `ConfiguracaoSistema` (Singleton), `RelatorioBuilder` (Builder) |
| Limiares vindos da configuração | `ConfiguracaoSistema.getLimiares()` |
| Níveis ATENÇÃO, ALERTA, CRÍTICO | `dominio/NivelAlerta`, `dominio/Limiares.classificar()` |
| Menu com as 5 opções e `Scanner` | `SistemaMonitoramento` |
| Testes AAA, `assertThrows` e Mockito | `src/test/java` (ver seção Testes) |
| Bônus | exportação do histórico consultado em CSV, `persistencia/ExportadorCsv` |

## Estrutura do projeto

```
territory-predictive-insights-fev/
├── pom.xml
├── README.md
├── src/main/java/
│   ├── SistemaMonitoramento.java      menu principal (main)
│   ├── dominio/                       ComponenteTerritorial, EstacaoMonitoramento, StatusComponente,
│   │                                  Sensor, SensorSimulado, SensorChuva, SensorNivelRio, SensorDrenagem,
│   │                                  TipoSensor, Medicao, Alerta, NivelAlerta, Limiares, DadosTerritorio,
│   │                                  EstrategiaPrevisao, PrevisaoMediaMovel, PrevisaoComparacao, Formato
│   ├── excecao/                       DadosInvalidosException, FonteIndisponivelException,
│   │                                  AcessoNegadoException, PersistenciaException
│   ├── persistencia/                  ConexaoBanco, MedicaoDAO, AlertaDAO, ExportadorCsv
│   └── servico/                       ServicoMonitoramento, FabricaPrevisao, ConfiguracaoSistema,
│                                      RelatorioBuilder, ResultadoPrevisao
└── src/test/java/
    ├── dominio/                       MedicaoTest, LimiaresTest, AlertaTest, DadosTerritorioTest,
    │                                  EstacaoMonitoramentoTest, SensorSimuladoTest,
    │                                  PrevisaoMediaMovelTest, PrevisaoComparacaoTest
    ├── servico/                       ServicoMonitoramentoTest, FabricaPrevisaoTest,
    │                                  ConfiguracaoSistemaTest, RelatorioBuilderTest
    └── persistencia/                  MedicaoDAOTest, AlertaDAOTest, ExportadorCsvTest
```

`LeiturasDeTeste` e `BancoDeTeste`, também em `src/test/java`, são classes de apoio que montam históricos e bancos em memória para os testes.

## Decisões da equipe

- **JDBC puro em vez de Spring Boot.** O sistema é um menu de console com duas tabelas. Com JDBC, todo o caminho da medição até o banco fica em código que conseguimos explicar linha a linha.
- **H2 em arquivo, não em memória.** Em memória o histórico sumiria ao fechar o programa, e o enunciado pede o contrário. O modo em memória continua disponível pela propriedade `banco.url` e é o que os testes dos DAOs usam.
- **Medição e alerta imutáveis, validados no construtor.** Um objeto `Medicao` só existe se o valor estiver na faixa do sensor. Por isso não há como gravar valor impossível no banco, nem pelo menu nem por outro caminho.
- **`FonteIndisponivelException` verificada, `DadosInvalidosException` não verificada.** Sensor fora do ar é esperado em campo, então o compilador obriga quem pede a leitura a tratar. Dado inválido é um valor que não deveria ter chegado até ali: quem valida lança e o menu captura.
- **Duas exceções além das pedidas.** `AcessoNegadoException` separa "código de acesso errado" de "dado inválido", e `PersistenciaException` embrulha a `SQLException` guardando a causa, para o menu mostrar uma mensagem útil sem depender de JDBC.
- **Limiares por território.** Cada lugar alaga com uma chuva diferente. Os limiares ficam em `DadosTerritorio`, que só aceita alteração com o código de acesso e recusa limites fora de ordem ou acima do que o sensor mede.
- **Mínimo de 5 leituras para prever.** Abaixo disso o serviço devolve "sem previsão", em vez de um número sem base.
- **Alerta repetido não é gravado de novo.** Rodar a previsão duas vezes sem leitura nova não traz informação nova, e alerta repetido ensina as pessoas a ignorá-lo.
- **O menu só conversa com o usuário.** Nenhuma regra de negócio e nenhum SQL ficam em `SistemaMonitoramento`. O serviço recebe os DAOs pelo construtor, o que permite testá-lo com Mockito.

## Padrões criacionais

Os três foram aplicados, cada um onde a criação de objetos era de fato o problema.

**Factory, em `FabricaPrevisao`.** O serviço precisa das estratégias de previsão, mas não deve saber quais existem nem como montar cada uma (a média móvel recebe uma janela, a comparação recebe um período). A fábrica guarda um mapa de nome para receita de construção e lê os parâmetros da configuração. Por que ali: é o único ponto em que o tipo concreto da estratégia importa. Se a criação ficasse no serviço ou no menu, cada estratégia nova exigiria um `if` a mais nesses lugares. Com a fábrica, basta registrar a receita e acrescentar o nome na configuração. `ServicoMonitoramento.rodarPrevisoes` só chama `FabricaPrevisao.criarAtivas(configuracao)` e trabalha com o tipo `EstrategiaPrevisao`.

**Singleton, em `ConfiguracaoSistema`.** Os limiares precisam ser os mesmos para o menu que os altera, para o serviço que compara a previsão com eles e para o relatório que os mostra. Com uma instância única e construtor privado, o limiar alterado na opção 4 vale na previsão seguinte, sem cópia que possa ficar desatualizada. Por que não nos DAOs ou no serviço: eles são criados com dependências diferentes nos testes (mock ou banco em memória), e um Singleton ali impediria essa troca.

**Builder, em `RelatorioBuilder`.** O status completo tem partes opcionais e em quantidade variável (território, limiares, cada equipamento, cada sensor, alertas). A tela "ver dados e limiares" usa só duas dessas partes. O builder monta passo a passo, e `construir()` recusa relatório sem título ou vazio. Por que não um construtor: seria uma lista longa de parâmetros, vários deles nulos.

## Testes

`mvn test` roda 116 testes JUnit 5, todos no formato Arrange, Act, Assert.

| O que o enunciado pede | Onde ver |
|---|---|
| Pelo menos 5 testes AAA | todas as classes de teste |
| `assertThrows` das exceções customizadas | `MedicaoTest`, `LimiaresTest`, `DadosTerritorioTest`, `SensorSimuladoTest`, `ServicoMonitoramentoTest`, entre outras |
| Mockito mockando o DAO | `ServicoMonitoramentoTest`: `MedicaoDAO` e `AlertaDAO` são mocks, e a lógica de alerta é testada sem banco |
| Testes de verdade | os valores esperados são calculados à mão no comentário de cada cenário; alterando a conta da média, o `>=` dos limiares ou a regra de não repetir alerta, os testes falham |

`MedicaoDAOTest` e `AlertaDAOTest` conferem o SQL em um H2 em memória, com um banco novo a cada teste. Eles não criam arquivo e não dependem do banco usado pelo programa.

## Reflexão crítica

**Quando o sistema deve se calar?** Quando não tem base. Com menos de 5 leituras de um sensor no banco, a opção 2 responde "sem previsão" e mostra quantas leituras faltam. Uma média de duas leituras pode tanto alarmar por causa de um pico isolado quanto tranquilizar no começo de uma tempestade, e os dois erros custam caro: o primeiro desgasta a confiança no alerta, o segundo deixa as pessoas desprevenidas. O sistema também se cala quando o sensor não responde ou devolve um valor impossível: essa leitura é descartada com aviso e não entra no histórico. E ele não repete um alerta sem leitura nova. Ainda assim, há um silêncio que ele não resolve: se o sensor ficar horas fora do ar, a previsão continua usando leituras antigas. O horário da última leitura aparece no status completo justamente para a pessoa perceber isso.

**O que os dados não representam?** Um pluviômetro e uma régua descrevem um ponto, não o território inteiro. A chuva pode ser forte a dois quarteirões da estação, e uma boca de lobo entupida alaga uma rua que nenhum sensor acompanha. Os dados também não dizem quem está em risco: uma casa com pessoa acamada e uma casa vazia têm a mesma leitura. Os limiares são números definidos por alguém e não uma verdade sobre o lugar; por isso podem ser alterados, com código de acesso. Por fim, as leituras aqui são simuladas. As previsões são contas simples sobre o passado recente e não conhecem o solo, a maré ou a previsão do tempo.

**Por que o alerta apoia a decisão em vez de decidir?** Porque quem conhece o território é a pessoa. Ela sabe se a ponte já está interditada, se é dia de feira na baixada, se o sensor foi consertado ontem. Por isso cada alerta explica o porquê (sensor, valor previsto, limite atingido, estratégia usada) e traz uma sugestão, nunca uma ordem. Com a explicação, a pessoa pode discordar: ver que as duas estratégias divergem, ou que a previsão se apoia em leituras velhas. Um sistema que mandasse evacuar sozinho erraria sem que ninguém pudesse conferir, e a responsabilidade pela decisão precisa ficar com quem responde por ela.

## Bônus

Exportação em CSV do histórico consultado (opção 3). Depois de listar as medições de um sensor ou os alertas emitidos, o menu oferece gravar o resultado na pasta `exportacoes`. O arquivo usa ponto e vírgula como separador, vírgula decimal e UTF-8 com marca de início, que é o formato esperado pelo Excel em português.

## Limitações conhecidas

- Os dados do território e os limiares alterados no menu valem até o programa ser fechado. Só medições e alertas são gravados no banco, que é o que o enunciado pede.
- Rodadas de leitura feitas em sequência recebem o horário real em que foram lidas, então várias aparecem com o mesmo horário. A ordem do histórico é garantida pelo número da medição.
- A simulação da tempestade recomeça a cada execução do programa.
- Antes da primeira leitura de uma execução, os sensores aparecem como "respondendo" no status, porque ainda não houve tentativa que falhasse.
- O código de acesso fica no código-fonte, por ser um projeto didático.

## Uso de IA

Usamos o Claude (Anthropic) como assistente no desenvolvimento. Ele ajudou a escrever e a revisar o código, e a conferir o projeto contra os critérios de avaliação.

Como revisamos:

- Configuramos o ambiente no Windows (JDK e Maven), compilamos o projeto com `mvn clean package` e executamos o menu seguindo o roteiro de demonstração.
- Dividimos o código pelas funções da tabela abaixo. Cada integrante leu a sua parte antes de commitar e fez os próprios commits, na própria conta do GitHub, com uma mensagem que explica o que o commit faz e por quê.

## Divisão de tarefas

| Integrante | RM | Responsabilidade |
|---|---|---|
| Bernardo Berwanger | 565776 | Domínio, exceções e persistência (sensores, estação, território, DAOs, exportação CSV) |
| João Vitor Angeloti Sena | 563473 | Previsões, alertas, padrões criacionais e testes (estratégias, serviço, Factory, Singleton, Builder) |

O menu principal (`SistemaMonitoramento`) e este README foram feitos pelos dois integrantes.
