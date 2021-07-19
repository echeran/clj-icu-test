package sqlbuilder;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.ArrayList;
import java.util.stream.Collectors;
public class Examples {
public static final String f1() {
HashMap<String,Object> tmp1 = new HashMap<String,Object>();
ArrayList<Object> tmp2 = new ArrayList<Object>();
tmp2.add(":foo");
tmp1.put(":from", tmp2);
ArrayList<Object> tmp3 = new ArrayList<Object>();
tmp3.add(":a");
tmp3.add(":b");
tmp3.add(":c");
tmp1.put(":select", tmp3);
ArrayList<Object> tmp4 = new ArrayList<Object>();
tmp4.add(":=");
tmp4.add(":f.a");
tmp4.add("baz");
tmp1.put(":where", tmp4);
final HashMap<String,Object> queryMap = tmp1;
return sqlbuilder.Core.format(queryMap);
}
}
