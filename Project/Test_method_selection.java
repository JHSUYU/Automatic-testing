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




public class Test_method_selection {
    public static void select_method(CHACallGraph cg,String change_info) {

        List<String> txt = new ArrayList<String>();
        String path = change_info;
        //将所有变更方法记录到txt数组中
        readTxt(path, txt);
        List<String> selection_method=new ArrayList<String>();
        List<String[]> res_method=new ArrayList<String[]>();
        //这个方法会遍历调用图，存储所有的方法依赖关系，包括直接依赖和间接依赖
        //res_method的每一项是包含两个字符串的数组，[method1，method2],表示method1直接或间接调用了method2
        iterate(cg,res_method);
        //对每一个changeinfo里的变更方法，遍历res_method数组，找到所有直接或者间接调用它的方法，将结果存储在selection_method里
        for(int i=0;i<txt.size();i++){
            for(int j=0;j<res_method.size();j++){
                if(res_method.get(j)[1].equals(txt.get(i))){
                    if(res_method.get(j)[0].contains("test")){
                        if(judge(selection_method,res_method.get(j)[0]))  //两个不同的变更方法可能会调用同一个方法，但该方法在结果里只用输出一次，judge函数保证没有重复
                            selection_method.add(res_method.get(j)[0]);
                    }
                }
            }
        }
        //我们只挑选测试方法，所以还要对selection_method做筛选，调用图中只有测试方法和init方法Pre结点为0，把Pre结点为0的方法都挑选出来
        //再剔除init方法，即可得到所有受变更影响的测试方法,最终结果存储在select_method里

        List<String> select_method=new ArrayList<String>();
        for(CGNode node: cg) {
            if(node.getMethod() instanceof ShrikeBTMethod) {
                ShrikeBTMethod method = (ShrikeBTMethod) node.getMethod();
                if("Application".equals(method.getDeclaringClass().getClassLoader().toString())) {
                    String classInnerName = method.getDeclaringClass().getName().toString();
                    String signature=method.getSignature();
                    String output=classInnerName+" "+signature;
                    for(int h=0;h<selection_method.size();h++){
                        if(selection_method.get(h).equals(output)){
                            Iterator<CGNode> Pre=cg.getPredNodes(node);
                            int cnt=0;
                            while(Pre.hasNext()){
                                CGNode temp2=Pre.next();
                                cnt++;
                            }
                            if(cnt==0  && !selection_method.get(h).contains("init")){
                                select_method.add(selection_method.get(h));
                            }

                        }
                    }

                }
            }


        }
        String output_path="./selection-method.txt";
        writeTxt(output_path,select_method);
    }
    //这个方法用来保证没有重复的方法依赖被添加到res当中
    public static boolean judge_res(List<String[]> res,String str1,String str2){
        for(int i=0;i<res.size();i++){
            if(res.get(i)[0].equals(str1) && res.get(i)[1].equals(str2)){
                return false;
            }

        }
        return true;
    }
    //从某一个方法节点出发，通过Succ找到所有它直接或间接调用的方法
    public static void iterate2(CHACallGraph cg,CGNode node,List<String[]> res_method, String output){
        Iterator<CGNode> node_Succ=cg.getSuccNodes(node);
        while(node_Succ.hasNext()){
            CGNode temp_node=node_Succ.next();
            ShrikeBTMethod method2 = (ShrikeBTMethod) temp_node.getMethod();
            if("Application".equals(method2.getDeclaringClass().getClassLoader().toString())){
                String signature2=method2.getSignature();
                String classInnerName2=method2.getDeclaringClass().getName().toString();
                String output2=classInnerName2+" "+signature2;
                String[] temp=new String[2];
                temp[0]=output;
                temp[1]=output2;
                if(judge_res(res_method,output,output2))
                {
                    res_method.add(temp);
                    iterate2(cg,temp_node,res_method,output);
                }
                else{
                    return ;
                }

            }

        }


    }
    //遍历图中所有方法结点，找到每一个点直接或间接调用的方法（这一步通过函数iterate2完成）
    public static void iterate(CHACallGraph cg,List<String[]> res_method){
        for (CGNode node : cg) {
            if (node.getMethod() instanceof ShrikeBTMethod) {
                ShrikeBTMethod method = (ShrikeBTMethod) node.getMethod();
                if ("Application".equals(method.getDeclaringClass().getClassLoader().toString())) {
                    String signature=method.getSignature();
                    String classInnerName=method.getDeclaringClass().getName().toString();
                    String output=classInnerName+" "+signature;
                    iterate2(cg,node,res_method,output);

                }

            }
        }



    }


    public static boolean judge_Pre(List<String[]> res, String suc){
        for(int i=0;i<res.size();i++){
            boolean a=(res.get(i)[0]==suc && !res.get(i)[1].contains("init"));
            boolean b=(res.get(i)[0]!=suc);

            if(!(a||b))
                return false;

        }
        return true;
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


    //读入变更信息，存储到数组中
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


    //防止重复的方法依赖关系出现在结果当中
    public static boolean judge(List<String> strings, String string) {
        for (int i = 0; i < strings.size(); i++) {
            if (strings.get(i).equals(string)) {
                return false;
            }

        }
        return true;

    }
}

