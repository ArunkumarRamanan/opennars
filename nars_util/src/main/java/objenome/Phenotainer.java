package objenome;

import javassist.*;
import objenome.problem.Problem;
import objenome.solution.SetMethodsGPEvolved;
import objenome.solution.dependency.Builder;
import objenome.solver.Solution;

import java.io.IOException;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * The container (phenotype) generated by an individual Objenome
 */
public class Phenotainer extends Container {
    public static long classSerial = 0;
    
    public final Map<Parameter, Object> parameterValues = new HashMap();
    public final Objenome objenome;
    public final Multitainer parent;

    public Phenotainer(Objenome o) {
        super(o.parentContext);        
        this.objenome = o;
        this.parent = o.parentContext;
        
        
        
        
        commit();
    }
    
    /** applies (current values of) the genes to the container for use by the next 
     *  instanced objects */
    public Phenotainer commit() {
        //remove all builders with ambiguosity
        Collection<String> toRemove = new ArrayList();
        for (Map.Entry<String, Builder> e : this.builders.entrySet()) {
            if (e.getValue() instanceof Problem)
                toRemove.add(e.getKey());
        }
        for (String s : toRemove) this.builders.remove(s);

        for (Solution g : objenome.genes.values()) {
            g.apply(this);
        }
        return this;
    }
    
    public void use(Parameter p, Object value) {
        parameterValues.put(p, value);
    }

    public <T> T get(Parameter p) {
        return (T) parameterValues.get(p);
    }

    @Override
    public String toString() {
        return parameterValues + ",  " + constructorDependencies.toString() + ",  " + setterDependencies.toString() + ", ";
    }
    
    
   @Override
    public <T> T get(final Class<? extends T> c) {
        if (Modifier.isAbstract(c.getModifiers())) {
            
            Builder existingBuilder = getBuilder(c);
            if (existingBuilder!=null) {
                return super.get(c);
            }
            
            try {
                Class<? extends T> implementation = instance(c);
                //TODO cache the implementation?
                use(c, implementation);
                return super.get(c);
                
            } catch (Exception ex) {                                
                ex.printStackTrace();
                throw new RuntimeException("Unable to implement " + c + "; " + ex.toString(), ex);
                
            }
        }
        
        return super.get(c);
    }    
    
    protected <T> Class<? extends T> instance(final Class<? extends T> c) throws CannotCompileException, NotFoundException, IOException, NoSuchFieldException, IllegalArgumentException, IllegalAccessException {
        
        CtClass parent = ClassPool.getDefault().get(c.getName()); 
        
        String newClassName = c.getName() + '_' + (classSerial++) + SetMethodsGPEvolved.DYNAMIC_SUFFIX;
        CtClass newImp = ClassPool.getDefault().makeClass(newClassName);
        

        newImp.setSuperclass(parent);
        
        CtConstructor[] parentConstructors = parent.getConstructors();
        if (parentConstructors.length > 1) {
            throw new RuntimeException("Phenotainer.instance() unable to decide >1 constructors in abstract class " + c);
        }
        if (parentConstructors.length == 0) {
            throw new RuntimeException("Phenotainer.instance() found no available constructors in abstract class " + c);
        }

        CtConstructor parentcon = parentConstructors[0];
        
        CtConstructor newCon = CtNewConstructor.make(parentcon.getParameterTypes(), parentcon.getExceptionTypes(), newImp);
        
        newImp.addConstructor(newCon);
        
        CtField pheno = new CtField(ClassPool.getDefault().get(Phenotainer.class.getName()), "pheno", newImp);
        pheno.setModifiers(Modifier.PUBLIC | Modifier.STATIC);        
        newImp.addField(pheno);
        
        
        
        
        for (CtMethod m : parent.getMethods()) {
            if (Modifier.isAbstract(m.getModifiers())) {
                //implement method in child
                CtMethod newMeth = new CtMethod(m.getReturnType(), m.getName(), m.getParameterTypes(), newImp);                
                newImp.addMethod(newMeth);
                newMeth.setBody("{ System.out.println(\"IMPLEMENTED\"); return 0d; }");
                
                
//                CtField gene = new CtField(ClassPool.getDefault().get("java.lang.Object"), "gene", newImp);
//                
//                newImp.addField(gene);
                
            }
        }
        
        //necessary to set non-abstract since it is by default
        newImp.setModifiers(newImp.getModifiers() & ~Modifier.ABSTRACT);
        
        
        Class newclass = newImp.toClass();
        newclass.getField("pheno").set(null, this);
        
        return newclass;
        
    }
    
}
