package Select;

import java.io.*;
import java.util.*;

import com.ibm.wala.classLoader.ShrikeBTMethod;
import com.ibm.wala.ipa.callgraph.AnalysisScope;
import com.ibm.wala.ipa.callgraph.CGNode;
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




public class Test_class_selection {
    public static void select_class(CHACallGraph cg,String change_info)  {
        List<String> txt = new ArrayList<String>();
        String change_path = change_info;
        readTxt(change_path, txt);  //这个方法会把变更文件里的每一个方法签名全部读入到txt数组中
        List<String> select_class=new ArrayList<String>();
        List<String[]> res_class=new ArrayList<String[]>();
        List<String> res_dot=new ArrayList<String>();
        /*这个方法将CHACallGraph里类的.dot依赖图存储在res_dot中，res_class的每一项是包含两个类签名的字符串class1和class2
        表示class1依赖于class2，存储了类的依赖关系。*/
        iterate_class(cg,res_class,res_dot);
       /* 对每一个变更的方法所在的类，遍历res_class数组，如果class2字符串等于变更类的签名，则class1添加到select_class中，
        找到所有依赖变更类的类*/
        for(int i=0;i<txt.size();i++){
            for(int j=0;j<res_class.size();j++){
                if(txt.get(i).substring(0,txt.get(i).indexOf(" ")).equals(res_class.get(j)[1])){
                    if(judge(select_class,res_class.get(j)[0])){ //judge方法保证结果没有重复的数组
                        select_class.add(res_class.get(j)[0]);
                    }

                }
            }
        }
        //将cg图里所有的方法签名存储到all_method里
        List <String> all_method=new ArrayList<String>();
        for(CGNode node: cg) {
            if(node.getMethod() instanceof ShrikeBTMethod) {
                ShrikeBTMethod method = (ShrikeBTMethod) node.getMethod();
                if("Application".equals(method.getDeclaringClass().getClassLoader().toString())) {
                    String classInnerName = method.getDeclaringClass().getName().toString();
                    String signature=method.getSignature();
                    String output=classInnerName+" "+signature;
                    if(judge(all_method,output))
                        all_method.add(output);

                }
            }


        }
        //select_class是所有受到影响的类，遍历所有方法签名，如果这个方法所在的类签名与受影响的类签名一样，就添加到temp_res中
        List<String> temp_res=new ArrayList<String>();
        for(int i=0;i<all_method.size();i++){
            for(int j=0;j<select_class.size();j++){
                if(all_method.get(i).substring(0,all_method.get(i).indexOf(" ")).equals(select_class.get(j))){
                    if(judge (temp_res,all_method.get(i)))
                        temp_res.add(all_method.get(i));
                }
            }

        }

        //我们只挑选测试方法，所以还要对temp_res做筛选，调用图中只有测试方法和init方法Pre结点为0，把Pre结点为0的方法都挑选出来
        //再剔除init方法，即可得到所有受变更影响的测试方法
        List<String> select_method=new ArrayList<String>();
        for(CGNode node: cg) {
            if(node.getMethod() instanceof ShrikeBTMethod) {
                ShrikeBTMethod method = (ShrikeBTMethod) node.getMethod();
                if("Application".equals(method.getDeclaringClass().getClassLoader().toString())) {
                    String classInnerName = method.getDeclaringClass().getName().toString();
                    String signature=method.getSignature();
                    String output=classInnerName+" "+signature;
                    for(int h=0;h<temp_res.size();h++){
                        if(temp_res.get(h).equals(output)){
                            Iterator<CGNode> Pre=cg.getPredNodes(node);
                            int cnt=0;
                            while(Pre.hasNext()){
                                CGNode temp2=Pre.next();
                                cnt++;
                            }
                            if(cnt==0  && !temp_res.get(h).contains("init") &&!temp_res.get(h).contains(";")){
                                select_method.add(temp_res.get(h));
                            }

                        }
                    }

                }
            }


        }
        //写入txt文件
        String output_path="./selection-class.txt";
        writeTxt(output_path,select_method);
    }

    public static boolean judge_res(List<String[]> res,String str1,String str2){
        for(int i=0;i<res.size();i++){
            if(res.get(i)[0].equals(str1) && res.get(i)[1].equals(str2)){
                return false;
            }

        }
        return true;
    }



    //iterate_class会通过所有方法的调用关系得到得到所有的类依赖关系，（调用通过调用图提供的succ接口完成）并存储在res_class中，res_dot是用来画图的
    public static void iterate_class(CHACallGraph cg,List<String[]> res_class,List<String> res_dot){
        for (CGNode node : cg) {
            if (node.getMethod() instanceof ShrikeBTMethod) {
                Iterator<CGNode> node_Succ=cg.getSuccNodes(node);
                ShrikeBTMethod method = (ShrikeBTMethod) node.getMethod();
                if ("Application".equals(method.getDeclaringClass().getClassLoader().toString())) {
                    String classInnerName = method.getDeclaringClass().getName().toString();
                    String signature = method.getSignature();
                    String content='"'+classInnerName+'"'+" -> ";
                    String content_2=classInnerName;
                    while(node_Succ.hasNext()){
                        CGNode temp_node=node_Succ.next();
                        ShrikeBTMethod method2 = (ShrikeBTMethod) temp_node.getMethod();
                        if("Application".equals(method2.getDeclaringClass().getClassLoader().toString())){
                            String className=method2.getDeclaringClass().getName().toString();
                            String signature2=method2.getSignature();
                            String content_3=className;
                            content=content+'"'+className+'"'+";\n";
                            if(judge(res_dot,content)){
                                String[] temp=new String[2];
                                temp[0]=content_2;
                                temp[1]=content_3;
                                res_class.add(temp);
                                res_dot.add(content);

                            }
                            content='"'+classInnerName+'"'+" -> ";
                        }

                    }



                }

            }
        }


    }



    //将结果输出到txt文件中
    public static void writeTxt(String txtPath,List<String> content){
        try {
            FileWriter fw = new FileWriter(txtPath);
            BufferedWriter bw = new BufferedWriter(fw);
            for(int i=0;i<content.size();i++){
                if(i==content.size()-1)
                    bw.write(content.get(i));
                else
                    bw.write(content.get(i)+"\n");
            }
            bw.close();
            fw.close();
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

    }


    //将变更信息存储到数组中
    public static List<String>  readTxt(String txtPath,List<String> res) {
        File file = new File(txtPath);
        if(file.isFile() && file.exists()){
            try {
                FileInputStream fileInputStream = new FileInputStream(file);
                InputStreamReader inputStreamReader = new InputStreamReader(fileInputStream);
                BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
                String text = null;
                while((text = bufferedReader.readLine()) != null ){
                    res.add(text);
                }
                return res;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    //防止重复的类依赖关系出现在结果当中
    public static boolean judge(List<String> strings, String string) {
        for (int i = 0; i < strings.size(); i++) {
            if (strings.get(i).equals(string)) {
                return false;
            }

        }
        return true;

    }
}


