package sqlbuilder;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.ArrayList;
import java.util.stream.Collectors;
public class Core {
public static final String castToStr(final Object x) {
return (String)x;
}
public static final String selectStr(final ArrayList<Object> select) {
return String.join(", ", select.stream().map(sqlbuilder.Core::castToStr).collect(Collectors.toList()));
}
public static final String fromStr(final ArrayList<Object> from) {
return String.join(", ", from.stream().map(sqlbuilder.Core::castToStr).collect(Collectors.toList()));
}
public static final String joinStr(final ArrayList<Object> join) {
return String.join(", ", join.stream().map(sqlbuilder.Core::castToStr).collect(Collectors.toList()));
}
public static final String whereStr(final Object join) {
if ((join instanceof List))
{
final ArrayList<Object> jj = (ArrayList)join;
return ("" + "(" + String.join(("" + " op "), jj.stream().map(sqlbuilder.Core::whereStr).collect(Collectors.toList())) + ")");
}
else
{
return (String)join;
}
}
public static final String groupByStr(final ArrayList<Object> join) {
return String.join(", ", join.stream().map(sqlbuilder.Core::castToStr).collect(Collectors.toList()));
}
public static final String havingStr(final Object having) {
return sqlbuilder.Core.whereStr(having);
}
public static final String format(final HashMap<String,Object> queryMap) {
final Object select = queryMap.get(":select");
final Object from = queryMap.get(":from");
final Object join = queryMap.get(":join");
final Object whereClause = queryMap.get(":where");
final Object groupBy = queryMap.get(":group-by");
final Object having = queryMap.get(":having");
String tmp1;
if ((select == null))
{
tmp1 = "";
}
else
{
tmp1 = ("" + "SELECT " + sqlbuilder.Core.selectStr((ArrayList)select));
}
String tmp2;
if ((from == null))
{
tmp2 = "";
}
else
{
tmp2 = ("" + " FROM " + sqlbuilder.Core.fromStr((ArrayList)from));
}
String tmp3;
if ((join == null))
{
tmp3 = "";
}
else
{
tmp3 = ("" + " JOIN " + sqlbuilder.Core.joinStr((ArrayList)join));
}
String tmp4;
if ((whereClause == null))
{
tmp4 = "";
}
else
{
tmp4 = ("" + " WHERE " + sqlbuilder.Core.whereStr(whereClause));
}
String tmp5;
if ((groupBy == null))
{
tmp5 = "";
}
else
{
tmp5 = ("" + " GROUP BY " + sqlbuilder.Core.groupByStr((ArrayList)groupBy));
}
String tmp6;
if ((having == null))
{
tmp6 = "";
}
else
{
tmp6 = ("" + " HAVING " + sqlbuilder.Core.havingStr(having));
}
return ("" + tmp1 + tmp2 + tmp3 + tmp4 + tmp5 + tmp6);
}
}
