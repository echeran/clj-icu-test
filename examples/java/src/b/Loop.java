package b;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.ArrayList;
public class Loop {
public static final int add(final int a, final int b) {
long i = 0;
while ((i < 10)) {
System.out.println(i);
i = (i + 1);
}
ArrayList<Integer> tmp2 = new ArrayList<Integer>();
tmp2.add(1);
tmp2.add(2);
tmp2.add(3);
for (int ii : tmp2) {
System.out.println(ii);
}
{
int x = 0;
{
while ((x < 10)) {
System.out.println((x + 1));
}
if (true)
{
return x;
}
else
{
return (2 * (3 + 4) * 5 * 6 * 7);
}
}
}
}
}