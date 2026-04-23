import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Random;
import java.util.Scanner;
import java.util.Set;

/**
 * 基于大模型的图结构文本分析系统.
 * 从文本文件中构建有向图，提供桥接词查询、最短路径、PageRank、随机游走等功能.
 *
 * @author zjq
 * @version 1.0
 */
public class Main {
  private static Map<String, Node> graph;  // 图：单词 -> 节点对象
  private static List<String> allWords;     // 所有单词列表
  @SuppressWarnings("PREDICTABLE_RANDOM")
  private static final Random RANDOM = new Random();
  private static final double D = 0.85;      // PageRank阻尼系数

  /**
   * 节点类（包含出边和入边）.
   */
  static class Node {
    String word;
    Map<String, Integer> outEdges;  // 出边：目标单词 -> 权重
    Map<String, Integer> inEdges;   // 入边：源单词 -> 权重
    int frequency;                   // 单词出现频率

    Node(String word) {
      this.word = word;
      this.outEdges = new HashMap<>();
      this.inEdges = new HashMap<>();
      this.frequency = 0;
    }

    void addOutEdge(String to) {
      outEdges.put(to, outEdges.getOrDefault(to, 0) + 1);
    }

    void addInEdge(String from) {
      inEdges.put(from, inEdges.getOrDefault(from, 0) + 1);
    }
  }

  /**
   * 程序入口.
   *
   * @param args 命令行参数，第一个参数为文件路径
   */
  public static void main(String[] args) {
    final Scanner scanner = new Scanner(System.in, StandardCharsets.UTF_8.name());
    graph = new LinkedHashMap<>();
    allWords = new ArrayList<>();

    System.out.println("===== 基于大模型的图结构文本分析系统 =====");

    String filePath;
    if (args.length > 0) {
      filePath = args[0];
      System.out.println("使用命令行参数指定的文件: " + filePath);
    } else {
      System.out.print("请输入文本文件路径: ");
      filePath = scanner.nextLine();
    }

    if (!buildGraphFromFile(filePath)) {
      System.out.println("文件读取失败，程序退出。");
      return;
    }

    while (true) {
      System.out.println("\n===== 请选择功能 =====");
      System.out.println("1. 展示有向图");
      System.out.println("2. 查询桥接词");
      System.out.println("3. 根据桥接词生成新文本");
      System.out.println("4. 计算最短路径");
      System.out.println("5. 计算PageRank值");
      System.out.println("6. 随机游走");
      System.out.println("7. 保存图为Mermaid格式");
      System.out.println("8. 保存图为DOT格式");
      System.out.println("0. 退出");
      System.out.print("请输入选择: ");

      int choice;
      try {
        choice = Integer.parseInt(scanner.nextLine());
      } catch (NumberFormatException e) {
        System.out.println("请输入有效数字！");
        continue;
      }

      // 提取变量到 switch 外部，避免作用域问题
      String word1;
      String word2;
      String word;
      String inputText;

      switch (choice) {
        case 1:
          showDirectedGraph();
          break;
        case 2:
          System.out.print("请输入第一个单词: ");
          word1 = scanner.nextLine().trim().toLowerCase();
          System.out.print("请输入第二个单词: ");
          word2 = scanner.nextLine().trim().toLowerCase();
          System.out.println(queryBridgeWords(word1, word2));
          break;
        case 3:
          System.out.print("请输入新文本: ");
          inputText = scanner.nextLine();
          System.out.println("生成的新文本: " + generateNewText(inputText));
          break;
        case 4:
          System.out.print("请输入第一个单词: ");
          word1 = scanner.nextLine().trim().toLowerCase();
          if (word1.isEmpty()) {
            System.out.println("请输入有效单词！");
            break;
          }
          System.out.print("请输入第二个单词 (直接回车则计算到所有节点的路径): ");
          word2 = scanner.nextLine().trim().toLowerCase();

          if (word2.isEmpty()) {
            System.out.println(calcAllShortestPaths(word1));
          } else {
            System.out.println(calcShortestPath(word1, word2));
          }
          break;
        case 5:
          System.out.print("请输入单词: ");
          word = scanner.nextLine().trim().toLowerCase();
          Double pr = calPageRank(word);
          if (pr != null) {
            System.out.printf("单词 '%s' 的PageRank值为: %.4f%n", word, pr);
          }
          break;
        case 6:
          String walk = randomWalk();
          System.out.println("随机游走路径: " + walk);

          // 确保output目录存在，并处理 mkdirs 返回值
          File outputDir = new File("output");
          if (!outputDir.exists()) {
            boolean created = outputDir.mkdirs();
            if (!created) {
              System.out.println("创建output目录失败！");
              break;
            }
          }

          try (PrintWriter writer = new PrintWriter(
              new OutputStreamWriter(new FileOutputStream("output/random_walk.txt"),
                  StandardCharsets.UTF_8))) {
            writer.println(walk);
            System.out.println("路径已保存到 output/random_walk.txt");
          } catch (IOException e) {
            System.out.println("保存文件失败: " + e.getMessage());
          }
          break;
        case 7:
          saveGraphAsMermaid();
          break;
        case 8:
          saveGraphAsDOT();
          break;
        case 0:
          System.out.println("感谢使用，再见！");
          scanner.close();
          return;
        default:
          System.out.println("无效选择，请重新输入！");
      }
    }
  }

  /**
   * 从文件读取文本并构建有向图.
   *
   * @param filePath 文件路径
   * @return 构建成功返回 true，失败返回 false
   */
  private static boolean buildGraphFromFile(String filePath) {
    try (BufferedReader reader = new BufferedReader(
        new InputStreamReader(new FileInputStream(filePath), StandardCharsets.UTF_8))) {
      StringBuilder content = new StringBuilder();
      String line;
      while ((line = reader.readLine()) != null) {
        content.append(line).append(" ");
      }

      // 处理文本：转小写，非字母字符替换为空格
      String text = content.toString().toLowerCase()
          .replaceAll("[^a-z]", " ");

      // 分割单词
      String[] words = text.split("\\s+");
      List<String> validWords = new ArrayList<>();
      for (String w : words) {
        if (!w.isEmpty()) {
          validWords.add(w);
        }
      }

      // 先创建所有节点（包括最后一个单词）
      for (String word : validWords) {
        if (!graph.containsKey(word)) {
          graph.put(word, new Node(word));
        }
        graph.get(word).frequency++;
      }

      // 构建边
      for (int i = 0; i < validWords.size() - 1; i++) {
        String from = validWords.get(i);
        String to = validWords.get(i + 1);

        Node fromNode = graph.get(from);
        Node toNode = graph.get(to);

        fromNode.addOutEdge(to);
        toNode.addInEdge(from);
      }

      // 更新所有单词列表
      allWords = new ArrayList<>(graph.keySet());

      System.out.println("图构建成功！共 " + graph.size() + " 个节点。");
      return true;

    } catch (FileNotFoundException e) {
      System.out.println("文件不存在: " + filePath);
      return false;
    } catch (IOException e) {
      System.out.println("读取文件失败: " + e.getMessage());
      return false;
    }
  }

  /**
   * 展示有向图（CLI格式）.
   */
  public static void showDirectedGraph() {
    System.out.println("\n===== 有向图结构 =====");
    System.out.println("格式: 节点 -> 目标节点(权重)");
    System.out.println("共 " + graph.size() + " 个节点\n");

    for (Map.Entry<String, Node> entry : graph.entrySet()) {
      String from = entry.getKey();
      Node node = entry.getValue();

      System.out.printf("%s (出现%d次) -> ", from, node.frequency);

      if (node.outEdges.isEmpty()) {
        System.out.println("(无出边)");
      } else {
        List<String> edgeStrs = new ArrayList<>();
        for (Map.Entry<String, Integer> edge : node.outEdges.entrySet()) {
          edgeStrs.add(edge.getKey() + "(" + edge.getValue() + ")");
        }
        System.out.println(String.join(", ", edgeStrs));
      }
    }
  }

  /**
   * 查询桥接词（支持多个桥接词）.
   *
   * @param word1 第一个单词
   * @param word2 第二个单词
   * @return 桥接词查询结果字符串
   */
  public static String queryBridgeWords(String word1, String word2) {
    // 检查单词是否存在
    boolean hasWord1 = graph.containsKey(word1);
    boolean hasWord2 = graph.containsKey(word2);

    if (!hasWord1 && !hasWord2) {
      return "No \"" + word1 + "\" and \"" + word2 + "\" in the graph!";
    } else if (!hasWord1) {
      return "No \"" + word1 + "\" in the graph!";
    } else if (!hasWord2) {
      return "No \"" + word2 + "\" in the graph!";
    }

    // 查找所有桥接词
    List<String> bridgeWords = new ArrayList<>();
    Node node1 = graph.get(word1);

    for (String candidate : node1.outEdges.keySet()) {
      Node candidateNode = graph.get(candidate);
      if (candidateNode != null && candidateNode.outEdges.containsKey(word2)) {
        bridgeWords.add(candidate);
      }
    }

    // 输出结果（符合实验手册格式）
    if (bridgeWords.isEmpty()) {
      return "No bridge words from \"" + word1 + "\" to \"" + word2 + "\"!";
    } else if (bridgeWords.size() == 1) {
      return "The bridge words from \"" + word1 + "\" to \"" + word2 + "\" is: \""
          + bridgeWords.get(0) + "\"";
    } else {
      StringBuilder sb = new StringBuilder();
      sb.append("The bridge words from \"").append(word1).append("\" to \"")
          .append(word2).append("\" are: ");

      for (int i = 0; i < bridgeWords.size(); i++) {
        if (i > 0) {
          if (i == bridgeWords.size() - 1) {
            sb.append(" and ");
          } else {
            sb.append(", ");
          }
        }
        sb.append("\"").append(bridgeWords.get(i)).append("\"");
      }
      return sb.toString();
    }
  }

  /**
   * 根据桥接词生成新文本（多个桥接词时随机选择）.
   *
   * @param inputText 输入文本
   * @return 插入桥接词后的新文本
   */
  public static String generateNewText(String inputText) {
    // 处理输入文本
    String processed = inputText.toLowerCase().replaceAll("[^a-z]", " ");
    String[] words = processed.split("\\s+");

    if (words.length <= 1) {
      return inputText;
    }

    List<String> result = new ArrayList<>();
    result.add(words[0]);

    for (int i = 0; i < words.length - 1; i++) {
      String current = words[i];
      String next = words[i + 1];

      // 查找所有桥接词
      List<String> bridgeWords = new ArrayList<>();
      if (graph.containsKey(current)) {
        Node currentNode = graph.get(current);
        for (String candidate : currentNode.outEdges.keySet()) {
          if (graph.containsKey(candidate)
              && graph.get(candidate).outEdges.containsKey(next)) {
            bridgeWords.add(candidate);
          }
        }
      }

      // 如果有桥接词，随机选择一个插入
      if (!bridgeWords.isEmpty()) {
        String bridge = bridgeWords.get(RANDOM.nextInt(bridgeWords.size()));
        result.add(bridge);
      }

      result.add(next);
    }

    return String.join(" ", result);
  }

  /**
   * 计算两个单词之间的最短路径（Dijkstra算法）.
   *
   * @param word1 起始单词
   * @param word2 目标单词
   * @return 最短路径字符串
   */
  public static String calcShortestPath(String word1, String word2) {
    // 检查单词是否存在
    if (!graph.containsKey(word1)) {
      return "单词 \"" + word1 + "\" 不存在于图中！";
    }
    if (!graph.containsKey(word2)) {
      return "单词 \"" + word2 + "\" 不存在于图中！";
    }

    // Dijkstra算法
    Map<String, Double> dist = new HashMap<>();
    final Map<String, String> prev = new HashMap<>();
    final Set<String> visited = new HashSet<>();

    // 初始化
    for (String node : graph.keySet()) {
      dist.put(node, Double.POSITIVE_INFINITY);
    }
    dist.put(word1, 0.0);

    // 优先队列
    PriorityQueue<String> pq = new PriorityQueue<>(
        (a, b) -> Double.compare(dist.get(a), dist.get(b))
    );
    pq.offer(word1);

    while (!pq.isEmpty()) {
      String current = pq.poll();

      if (visited.contains(current)) {
        continue;
      }
      visited.add(current);

      if (current.equals(word2)) {
        break;
      }

      Node currentNode = graph.get(current);
      if (currentNode == null) {
        continue;
      }

      for (Map.Entry<String, Integer> edge : currentNode.outEdges.entrySet()) {
        String neighbor = edge.getKey();
        double weight = edge.getValue();
        double newDist = dist.get(current) + weight;

        if (newDist < dist.get(neighbor)) {
          dist.put(neighbor, newDist);
          prev.put(neighbor, current);
          pq.offer(neighbor);
        }
      }
    }

    // 检查是否可达
    if (dist.get(word2) == Double.POSITIVE_INFINITY) {
      return "单词 \"" + word1 + "\" 和 \"" + word2 + "\" 之间不可达！";
    }

    // 重建路径
    List<String> path = new ArrayList<>();
    String current = word2;
    while (current != null) {
      path.add(0, current);
      current = prev.get(current);
    }

    return "最短路径: " + String.join(" → ", path) + "\n路径长度: " + dist.get(word2);
  }

  /**
   * 计算从一个单词到所有其他单词的最短路径.
   *
   * @param source 源单词
   * @return 所有最短路径字符串
   */
  public static String calcAllShortestPaths(String source) {
    if (!graph.containsKey(source)) {
      return "单词 \"" + source + "\" 不存在于图中！";
    }

    StringBuilder result = new StringBuilder();
    result.append("从 \"").append(source).append("\" 到所有节点的最短路径：\n");

    for (String target : graph.keySet()) {
      if (target.equals(source)) {
        continue;
      }

      // 对每个目标节点运行Dijkstra
      Map<String, Double> dist = new HashMap<>();
      final Map<String, String> prev = new HashMap<>();
      final Set<String> visited = new HashSet<>();

      for (String node : graph.keySet()) {
        dist.put(node, Double.POSITIVE_INFINITY);
      }
      dist.put(source, 0.0);

      PriorityQueue<String> pq = new PriorityQueue<>(
          (a, b) -> Double.compare(dist.get(a), dist.get(b))
      );
      pq.offer(source);

      while (!pq.isEmpty()) {
        String current = pq.poll();

        if (visited.contains(current)) {
          continue;
        }
        visited.add(current);

        if (current.equals(target)) {
          break;
        }

        Node currentNode = graph.get(current);
        if (currentNode == null) {
          continue;
        }

        for (Map.Entry<String, Integer> edge : currentNode.outEdges.entrySet()) {
          String neighbor = edge.getKey();
          double weight = edge.getValue();
          double newDist = dist.get(current) + weight;

          if (newDist < dist.get(neighbor)) {
            dist.put(neighbor, newDist);
            prev.put(neighbor, current);
            pq.offer(neighbor);
          }
        }
      }

      result.append("  → ").append(target).append(": ");
      if (dist.get(target) == Double.POSITIVE_INFINITY) {
        result.append("不可达\n");
      } else {
        // 重建路径
        List<String> path = new ArrayList<>();
        String current = target;
        while (current != null) {
          path.add(0, current);
          current = prev.get(current);
        }
        result.append("长度 ").append(dist.get(target)).append(" (")
            .append(String.join(" → ", path)).append(")\n");
      }
    }

    return result.toString();
  }

  /**
   * 计算单词的PageRank值.
   *
   * @param word 要查询的单词
   * @return PageRank值，如果单词不存在则返回 null
   */
  public static Double calPageRank(String word) {
    if (!graph.containsKey(word)) {
      System.out.println("单词 \"" + word + "\" 不存在于图中！");
      return null;
    }

    int n = graph.size();
    Map<String, Double> pr = new HashMap<>();

    // 使用TF-IDF归一化作为初始PR值
    // 计算总词频
    int totalFreq = 0;
    for (Node node : graph.values()) {
      totalFreq += node.frequency;
    }

    // 计算每个单词的IDF（入链数量作为文档频率）
    Map<String, Double> idf = new HashMap<>();
    for (Map.Entry<String, Node> entry : graph.entrySet()) {
      String node = entry.getKey();
      Node nodeObj = entry.getValue();
      // 入度数量作为文档频率（被多少个不同单词指向）
      int inDegree = nodeObj.inEdges.size();
      // IDF = log(总节点数 / (入度数量 + 1))
      double idfValue = Math.log((double) n / (inDegree + 1));
      idf.put(node, idfValue);
    }

    // 计算TF-IDF总和用于归一化
    double totalTfidf = 0.0;
    Map<String, Double> tfidf = new HashMap<>();
    for (Map.Entry<String, Node> entry : graph.entrySet()) {
      String node = entry.getKey();
      Node nodeObj = entry.getValue();
      // TF = 词频 / 总词频
      double tf = (double) nodeObj.frequency / totalFreq;
      double tfidfValue = tf * idf.get(node);
      tfidf.put(node, tfidfValue);
      totalTfidf += tfidfValue;
    }

    // 使用TF-IDF归一化作为初始PR值
    for (String node : graph.keySet()) {
      double initValue = tfidf.get(node) / totalTfidf;
      pr.put(node, initValue);
    }

    // 迭代计算PageRank
    int maxIterations = 100;
    double minError = 1e-10;

    for (int iter = 0; iter < maxIterations; iter++) {
      Map<String, Double> newPr = new HashMap<>();

      // 计算悬挂节点（出度为0）的PR值总和
      double sinkSum = 0.0;
      for (Node node : graph.values()) {
        if (node.outEdges.isEmpty()) {
          sinkSum += pr.get(node.word);
        }
      }

      // 计算新的PR值
      for (Map.Entry<String, Node> entry : graph.entrySet()) {
        String node = entry.getKey();
        Node currentNode = entry.getValue();
        double sum = 0.0;

        // 从指向当前节点的节点累加
        for (String source : currentNode.inEdges.keySet()) {
          Node sourceNode = graph.get(source);
          int weight = sourceNode.outEdges.get(node);
          int outDegree = sourceNode.outEdges.size();
          if (outDegree > 0) {
            sum += pr.get(source) * weight / outDegree;
          }
        }

        // PageRank公式
        double newValue = (1 - D) / n + D * (sum + sinkSum / n);
        newPr.put(node, newValue);
      }

      // 检查收敛
      double error = 0.0;
      for (String node : graph.keySet()) {
        error += Math.abs(newPr.get(node) - pr.get(node));
      }
      pr = newPr;

      if (error < minError) {
        break;
      }
    }

    return pr.get(word);
  }

  /**
   * 随机游走.
   *
   * @return 随机游走路径字符串
   */
  public static String randomWalk() {
    if (graph.isEmpty()) {
      return "图为空，无法进行随机游走！";
    }

    List<String> visitedNodes = new ArrayList<>();
    final Set<String> visitedEdges = new HashSet<>();

    // 随机选择起始节点
    String currentNode = allWords.get(RANDOM.nextInt(allWords.size()));
    visitedNodes.add(currentNode);

    System.out.println("随机游走开始，按回车键停止...");

    // 创建线程检测键盘输入
    final boolean[] stop = {false};
    Thread inputThread = new Thread(() -> {
      try {
        System.in.read();
        stop[0] = true;
        System.out.println("\n用户手动停止游走。");
      } catch (IOException e) {
        // 异常不影响核心流程，无需处理
      }
    });
    inputThread.setDaemon(true);
    inputThread.start();

    while (!stop[0]) {
      Node currentNodeObj = graph.get(currentNode);

      // 检查是否有出边
      if (currentNodeObj == null || currentNodeObj.outEdges.isEmpty()) {
        System.out.println("节点 \"" + currentNode + "\" 无出边，游走结束。");
        break;
      }

      // 获取所有出边
      List<String> neighbors = new ArrayList<>(currentNodeObj.outEdges.keySet());

      // 随机选择下一个节点
      String nextNode = neighbors.get(RANDOM.nextInt(neighbors.size()));

      // 检查是否重复边
      String edge = currentNode + "->" + nextNode;
      if (visitedEdges.contains(edge)) {
        System.out.println("遇到重复边 \"" + edge + "\"，游走结束。");
        break;
      }

      // 记录边和节点
      visitedEdges.add(edge);
      visitedNodes.add(nextNode);
      currentNode = nextNode;

      // 输出当前路径
      System.out.println("当前路径: " + String.join(" ", visitedNodes));

      // 短暂暂停
      try {
        Thread.sleep(500);
      } catch (InterruptedException e) {
        break;
      }
    }

    return String.join(" ", visitedNodes);
  }

  /**
   * 生成Mermaid格式的图并保存为.md文件（可在VS Code中预览）.
   */
  public static void saveGraphAsMermaid() {
    try {
      // 确保output目录存在
      File outputDir = new File("output");
      if (!outputDir.exists()) {
        boolean created = outputDir.mkdirs();
        if (!created) {
          System.out.println("创建output目录失败！");
          return;
        }
      }

      // 创建Mermaid格式文件，指定UTF-8编码
      try (PrintWriter writer = new PrintWriter(new OutputStreamWriter(
          new FileOutputStream("output/graph.md"), StandardCharsets.UTF_8))) {

        writer.println("# 有向图结构");
        writer.println();
        writer.println("```mermaid");
        writer.println("graph LR");
        writer.println("  %% 定义节点样式");
        writer.println("  classDef default fill:#lightblue,stroke:#333,stroke-width:2px;");
        writer.println();

        // 添加所有节点（带出现次数）
        for (Map.Entry<String, Node> entry : graph.entrySet()) {
          String node = entry.getKey();
          Node n = entry.getValue();
          // 将节点名中的特殊字符替换为安全字符
          String safeNode = node.replaceAll("[^a-zA-Z0-9]", "_");
          writer.printf("  %s[\"%s<br/>出现%d次\"]%n", safeNode, node, n.frequency);
        }
        writer.println();

        // 添加所有边
        for (Map.Entry<String, Node> entry : graph.entrySet()) {
          String from = entry.getKey();
          Node node = entry.getValue();
          String safeFrom = from.replaceAll("[^a-zA-Z0-9]", "_");

          for (Map.Entry<String, Integer> edge : node.outEdges.entrySet()) {
            String to = edge.getKey();
            int weight = edge.getValue();
            String safeTo = to.replaceAll("[^a-zA-Z0-9]", "_");
            writer.printf("  %s -->|%d| %s%n", safeFrom, weight, safeTo);
          }
        }

        writer.println("```");
      }

      System.out.println("Mermaid图已保存到 output/graph.md");
      System.out.println("在VS Code中打开此文件后点击右上角'预览'即可看到图形！");

    } catch (IOException e) {
      System.out.println("保存失败: " + e.getMessage());
    }
  }

  /**
   * 生成DOT格式文件（可在VS Code中用Graphviz Preview插件预览）.
   */
  public static void saveGraphAsDOT() {
    try {
      // 确保output目录存在
      File outputDir = new File("output");
      if (!outputDir.exists()) {
        boolean created = outputDir.mkdirs();
        if (!created) {
          System.out.println("创建output目录失败！");
          return;
        }
      }

      // 生成DOT格式的图描述文件
      StringBuilder dot = new StringBuilder();
      dot.append("// Directed Graph Structure\n");
      dot.append("// Open in VS Code with Graphviz Preview plugin\n\n");
      dot.append("digraph G {\n");
      dot.append("  // Graph settings\n");
      dot.append("  rankdir=LR;\n");
      dot.append("  node [shape=box, style=filled, fillcolor=lightblue, fontname=\"SimHei\"];\n");
      dot.append("  edge [color=gray, fontsize=10];\n");
      dot.append("\n");

      // 添加所有节点
      dot.append("  // Nodes\n");
      for (Map.Entry<String, Node> entry : graph.entrySet()) {
        String node = entry.getKey();
        Node n = entry.getValue();
        String safeNode = node.replace("\"", "\\\"");
        dot.append(String.format("  \"%s\" [label=\"%s\\n(freq:%d)\"];%n",
            safeNode, safeNode, n.frequency));
      }
      dot.append("\n");

      // 添加所有边
      dot.append("  // Edges (with weights)\n");
      for (Map.Entry<String, Node> entry : graph.entrySet()) {
        String from = entry.getKey();
        Node node = entry.getValue();
        String safeFrom = from.replace("\"", "\\\"");

        for (Map.Entry<String, Integer> edge : node.outEdges.entrySet()) {
          String to = edge.getKey();
          int weight = edge.getValue();
          String safeTo = to.replace("\"", "\\\"");
          dot.append(String.format("  \"%s\" -> \"%s\" [label=\"%d\"];%n",
              safeFrom, safeTo, weight));
        }
      }
      dot.append("}\n");

      // 写入DOT文件（指定UTF-8编码）
      String dotFile = "output/graph.dot";
      try (OutputStreamWriter osw = new OutputStreamWriter(
          new FileOutputStream(dotFile), StandardCharsets.UTF_8);
           PrintWriter writer = new PrintWriter(osw)) {
        writer.print(dot.toString());
      }

      System.out.println("DOT file saved: " + dotFile);
      System.out.println("Open in VS Code, right-click and select \"Preview Dot\" to view graph!");

    } catch (IOException e) {
      System.out.println("Save failed: " + e.getMessage());
    }
  }
}