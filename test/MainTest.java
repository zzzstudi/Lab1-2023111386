import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

public class MainTest {

  // 用于 T1, T3, T4 的图（唯一桥接词）
  private Map<String, Main.Node> singleBridgeGraph;
  // 用于 T2 的图（多个桥接词）
  private Map<String, Main.Node> multiBridgeGraph;
  // 用于 T7 的图（无桥接词）
  private Map<String, Main.Node> noBridgeGraph;

  @BeforeEach
  void setUp() {
    // 构建唯一桥接词图：hello -> a -> world
    singleBridgeGraph = new LinkedHashMap<>();
    Main.Node hello1 = new Main.Node("hello");
    Main.Node a = new Main.Node("a");
    Main.Node world1 = new Main.Node("world");
    hello1.addOutEdge("a");
    a.addOutEdge("world");
    singleBridgeGraph.put("hello", hello1);
    singleBridgeGraph.put("a", a);
    singleBridgeGraph.put("world", world1);

    // 构建多个桥接词图：hello -> a -> world 和 hello -> b -> world
    multiBridgeGraph = new LinkedHashMap<>();
    Main.Node hello2 = new Main.Node("hello");
    Main.Node a2 = new Main.Node("a");
    Main.Node b = new Main.Node("b");
    Main.Node world2 = new Main.Node("world");
    hello2.addOutEdge("a");
    a2.addOutEdge("world");
    hello2.addOutEdge("b");
    b.addOutEdge("world");
    multiBridgeGraph.put("hello", hello2);
    multiBridgeGraph.put("a", a2);
    multiBridgeGraph.put("b", b);
    multiBridgeGraph.put("world", world2);

    // 构建无桥接词图：hello -> world（直接相连）
    noBridgeGraph = new LinkedHashMap<>();
    Main.Node hello3 = new Main.Node("hello");
    Main.Node world3 = new Main.Node("world");
    hello3.addOutEdge("world");
    noBridgeGraph.put("hello", hello3);
    noBridgeGraph.put("world", world3);
  }

  // T1: 有效等价类 - 唯一桥接词，小写输入
  @Test
  void testT1_SingleBridge_Lowercase() {
    Main.initForTest(singleBridgeGraph, new ArrayList<>(singleBridgeGraph.keySet()));
    String result = Main.queryBridgeWords("hello", "world");
    assertEquals("The bridge words from \"hello\" to \"world\" is: \"a\"", result);
  }

  // T2: 有效等价类 - 多个桥接词，小写输入
  @Test
  void testT2_MultipleBridges_Lowercase() {
    Main.initForTest(multiBridgeGraph, new ArrayList<>(multiBridgeGraph.keySet()));
    String result = Main.queryBridgeWords("hello", "world");
    assertTrue(result.contains("\"a\""));
    assertTrue(result.contains("\"b\""));
    assertTrue(result.contains("are:"));
  }

  // T3: 有效等价类 - 唯一桥接词，大写输入
  @Test
  void testT3_SingleBridge_Uppercase() {
    Main.initForTest(singleBridgeGraph, new ArrayList<>(singleBridgeGraph.keySet()));
    String result = Main.queryBridgeWords("HELLO", "WORLD");
    assertEquals("The bridge words from \"hello\" to \"world\" is: \"a\"", result);
  }

  // T4: 有效等价类 - 唯一桥接词，混合大小写输入
  @Test
  void testT4_SingleBridge_MixedCase() {
    Main.initForTest(singleBridgeGraph, new ArrayList<>(singleBridgeGraph.keySet()));
    String result = Main.queryBridgeWords("HeLlO", "WoRlD");
    assertEquals("The bridge words from \"hello\" to \"world\" is: \"a\"", result);
  }

  // T5: 无效等价类 - word1 不存在
  @Test
  void testT5_Word1NotExist() {
    Main.initForTest(singleBridgeGraph, new ArrayList<>(singleBridgeGraph.keySet()));
    String result = Main.queryBridgeWords("notexist", "world");
    assertEquals("No \"notexist\" in the graph!", result);
  }

  // T6: 无效等价类 - word2 不存在
  @Test
  void testT6_Word2NotExist() {
    Main.initForTest(singleBridgeGraph, new ArrayList<>(singleBridgeGraph.keySet()));
    String result = Main.queryBridgeWords("hello", "notexist");
    assertEquals("No \"notexist\" in the graph!", result);
  }

  // T7: 无效等价类 - 无桥接词
  @Test
  void testT7_NoBridge() {
    Main.initForTest(noBridgeGraph, new ArrayList<>(noBridgeGraph.keySet()));
    String result = Main.queryBridgeWords("hello", "world");
    assertEquals("No bridge words from \"hello\" to \"world\"!", result);
  }
}