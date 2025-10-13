### Задача 1 (ветка main)
![message_to_coord.png](img/main/message_to_coord.png)
![sniffer_monitor.png](img/main/sniffer_monitor.png)
### Практика 3, задача 1б (ветка practice_03)

    1. Вместо передачи имён через аргумент, координатор получает имена вычислителей через DF.
    2. Координатор будет периодически обновлять список имён.

Координатор автоматически ищет агентов-вычислители через Directory Facilitator (DF).
Публикуем тип сервиса "calculator" для фильтрации (сервис нужен DF для взаимодействия с нашим сервисом вычислений).


```java
DFAgentDescription template = new DFAgentDescription();
ServiceDescription sdTemplate = new ServiceDescription();
sdTemplate.setType("calculator");
template.addServices(sdTemplate);
```


![df_gui.png](img/practice_03/df_gui.png)
![service_for_agent.png](img/practice_03/service_for_agent.png)