<!DOCTYPE html>
<html>
<head>
    <meta charset="utf‐8">
    <title>Hello World!</title>
</head>
<body>
Hello ${name}!
<br/>
<#--遍历数据模型中的list-->
<table>
    <tr>
        <td>序号</td>
        <td>姓名</td>
        <td>年龄</td>
        <td>钱包</td>
        <td>出生日期</td>
    </tr>
    <#--stus不为空-->
    <#if stus??>
        <#list stus as stu>
            <tr>
                <td>${stu_index + 1}</td>
                <td <#if stu.name =='小明'>style="background: aqua"</#if>>${stu.name}</td>
                <td>${stu.age}</td>
                <td <#if stu.money gt 500>style="background: aqua"</#if>>${stu.money}</td>
<#--                <td>${stu.birthday?date}</td>-->
                <td>${stu.birthday?string("yyyy年MM月")}</td>
            </tr>
        </#list>
        学生个数：${stus?size}
    </#if>
</table>
<br/>
<#--遍历数据模型中的map数据:第一种方法-->
    姓名：${(stuMap['stu1'].name)!''}<br/>
    年龄：${(stuMap['stu1'].age)!''}<br/>
    姓名：${(stuMap.stu2.name)!''}<br/>
    年龄：${(stuMap.stu2.age)!''}<br/>
<#--遍历数据模型中的map数据:第二种方法-->
<#list stuMap?keys as key>
    姓名：${stuMap[key].name}<br/>
    年龄：${stuMap[key].age}<br/>
</#list>
${point?c}
<br/>
<#assign text="{'bank':'工商银行','account':'10101920201920212'}" />
<#assign data=text?eval />
开户行：${data.bank} 账号：${data.account}
</body>
</html>