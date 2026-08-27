import net.minecraft.component.type.JukeboxPlayableComponent;
public class TestJukebox {
    public static void main(String[] args) {
        for (java.lang.reflect.Method m : JukeboxPlayableComponent.class.getMethods()) {
            System.out.println(m.getName() + " -> " + m.getReturnType().getName());
        }
    }
}
