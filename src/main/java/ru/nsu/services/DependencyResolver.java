package ru.nsu.services;

import org.jgrapht.alg.cycle.CycleDetector;
import org.jgrapht.graph.DefaultDirectedGraph;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.traverse.TopologicalOrderIterator;
import ru.nsu.model.BeanDefinition;

import javax.inject.Inject;
import javax.inject.Named;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Класс, созданный для сортировки бинов по количеству зависимостей
 * и для проверки всех бинов на циклические зависимости.
 */
public class DependencyResolver {

    private final Map<String, BeanDefinition> beanDefinitions;

    /**
     * Конструктор класса.
     *
     * @param beanDefinitions отношение из названий бинов в их модели.
     */
    public DependencyResolver(Map<String, BeanDefinition> beanDefinitions) {
        this.beanDefinitions = beanDefinitions;
    }

    /**
     * Ключевой метод, который создаёт граф, использующийся для топологической
     * сортировки бинов по количеству зависимостей, а после этого граф проверяется на наличие цикла.
     * Если цикл присутствует - то выбрасывается исключение с этой информацией.
     *
     * @return отсортированный список названий бинов.
     */
    public List<String> resolveDependencies() {
        // Создание направленного графа
        DefaultDirectedGraph<String, DefaultEdge> graph = new DefaultDirectedGraph<>(DefaultEdge.class);

        // Добавление вершин (бинов) в граф
        beanDefinitions.keySet().forEach(graph::addVertex);

        // Добавление рёбер на основе зависимостей
        beanDefinitions.forEach((beanName, beanDefinition) -> addDependenciesToGraph(beanName, beanDefinition, graph));

        CycleDetector<String, DefaultEdge> cycleDetector = new CycleDetector<>(graph);
        if (cycleDetector.detectCycles()){
            throw new RuntimeException("Detected cyclic dependencies among beans");
        }

        // Топологическая сортировка
        TopologicalOrderIterator<String, DefaultEdge> orderIterator = new TopologicalOrderIterator<>(graph);
        List<String> sortedBeans = new ArrayList<>();
        orderIterator.forEachRemaining(sortedBeans::add);

        return sortedBeans;
    }

    /**
     * Добавляет к текущему графу новую зависимость (ребро) из переданной модели.
     * Сначала проверяются внедряемые поля, затем внедряемый поля-провайдеры,
     * а затем и параметры конструктора, если через них проходит внедрение зависимостей.
     *
     * @param beanName имя бина.
     * @param beanDefinition модель бина.
     * @param graph исследуемый граф.
     */
    private void addDependenciesToGraph(String beanName, BeanDefinition beanDefinition, DefaultDirectedGraph<String, DefaultEdge> graph) {
        // Исследуем injectedFields
        if (beanDefinition.getInjectedFields() != null) {
            for (Field field : beanDefinition.getInjectedFields()) {
                Named namedAnnotation = field.getAnnotation(Named.class);
                if (namedAnnotation != null && graph.containsVertex(namedAnnotation.value())) {
                    graph.addEdge(beanName, namedAnnotation.value());
                }
            }
        }

        // Исследуем injectedProviderFields
        if (beanDefinition.getInjectedProviderFields() != null) {
            for (Field field : beanDefinition.getInjectedProviderFields()) {
                Named namedAnnotation = field.getAnnotation(Named.class);
                if (namedAnnotation != null && graph.containsVertex(namedAnnotation.value())) {
                    graph.addEdge(beanName, namedAnnotation.value());
                }
            }
        }

        // Внутри addDependenciesToGraph
        Constructor<?> constructor = beanDefinition.getConstructor();
        if (constructor != null && constructor.isAnnotationPresent(Inject.class)) {
            for (Parameter parameter : constructor.getParameters()) {
                Named named = parameter.getAnnotation(Named.class);
                String depName = (named != null) ? named.value() : parameter.getType().getSimpleName();
                if (graph.containsVertex(depName)) {
                    graph.addEdge(beanName, depName);
                }
            }
        }
    }
}
