package Select;

import java.io.*;
import java.util.*;

import com.ibm.wala.ipa.callgraph.AnalysisScope;
import com.ibm.wala.ipa.callgraph.Entrypoint;
import com.ibm.wala.ipa.callgraph.cha.CHACallGraph;
import com.ibm.wala.ipa.callgraph.impl.AllApplicationEntrypoints;
import com.ibm.wala.ipa.cha.ClassHierarchy;
import com.ibm.wala.ipa.cha.ClassHierarchyException;
import com.ibm.wala.ipa.cha.ClassHierarchyFactory;
import com.ibm.wala.shrikeCT.InvalidClassFileException;
import com.ibm.wala.util.CancelException;
import com.ibm.wala.util.config.AnalysisScopeReader;
import com.ibm.wala.types.ClassLoaderReference;


public class Test_Selection {
    //获得所有的class文件
    public static List<File> listFiles(File rootFile,List<File> fileList) throws IOException{
        File[] allFiles = rootFile.listFiles();
        for(File file : allFiles){
            if(file.isDirectory()){
                listFiles(file, fileList);
            }else{
                String path=file.getPath();
                if(path.endsWith(".class"))
                    fileList.add(file);
            }
        }
        return fileList;
    }
    //解析输入的命令行

    public static List<String> parse_input(String input){
        List<String> res=new ArrayList<String>();
        int index=0;
        //判断是按类还是按方法选择
        res.add(input.charAt(1)+"");
        for(int i=0;i<input.length();i++) {
            if ((input.charAt(i) + "").equals(" ") && i > 2) {
                index = i;
                break;
            }
        }

        //target文件路径
        res.add(input.substring(3,index));
        //change_info文件路径
        res.add(input.substring(index+1,input.length()));
        return res;
    }

    public static void main(String[] args) throws IOException,ClassHierarchyException, CancelException, InvalidClassFileException{
        /*String input="-c /Users/lizhenyu/Downloads/Automated-testing/ClassicAutomatedTesting/2-DataLog/target /Users/lizhenyu/Downloads/Automated-testing/ClassicAutomatedTesting/2-DataLog/data/change_info.txt";
        List<String> res=parse_input(input);
        String type=res.get(0);
        String Target_path=res.get(1);
        String change_info=res.get(2);*/

        String type=args[0];
        String Target_path=args[1];
        String change_info=args[2];

        ClassLoader classloader = Test_Selection.class.getClassLoader();
        AnalysisScope scope = AnalysisScopeReader.readJavaScope("scope.txt", new File("exclusion.txt"), classloader);
        File rootfile = new File(Target_path);
        List<File> class_list = new ArrayList<File>();
        class_list = listFiles(rootfile, class_list);//这个方法会得到目标目录下所有的.class文件
        //将所有的.class文件全部添加到分析域中
        for (File file : class_list)
            scope.addClassFileToScope(ClassLoaderReference.Application, file);
        //生成类层次关系对象
        ClassHierarchy cha = ClassHierarchyFactory.makeWithRoot(scope);
        // 生成进入点
        Iterable<Entrypoint> eps = new AllApplicationEntrypoints(scope, cha);
        // 利用CHA算法构建调用图
        CHACallGraph cg = new CHACallGraph(cha);
        //调用图初始化
        cg.init(eps);
        if(type.equals("-c")){
            //挑选类级
            Test_class_selection.select_class(cg,change_info);
        }
        else{
            //挑选方法级
            Test_method_selection.select_method(cg,change_info);
        }

    }

}

