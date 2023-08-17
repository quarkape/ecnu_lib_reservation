# 图书馆座位预约系统（盛卡恩空间）自动预约脚本

- 彩蛋：在抓包过程中，可以会发现一些有意思的 API，稍加利用，可以做很多有趣的事情~

# 效果

![每日预约成功会收到通知信息](https://raw.githubusercontent.com/quarkape/ecnu-library-book/main/imgs/03.jpg)





# :school: 支持多所高校的空间预约

- 支持盛卡恩空间预约系统，根据[官网](http://skalibrary.cn/)介绍，全国余90所高校的使用了该系统
- 该系统包括空间预约和通道闸机两个部分，有的学校两个都用了，有的学校只用了通道闸机。
- 官网给出的案例介绍了90余所高校的使用情况，可以在`material`目录下的`schools.txt`中查到。
- 如果图书馆预约系统是不是下面这个界面，那么大概率本脚本可用。

![](https://raw.githubusercontent.com/quarkape/ecnu-lib-auto-book/main/imgs/02.png)



# 使用教程

> 1. 先把`Seatreservation.java`和`SendEmail.java`两个文件需要配置好的参数都配置好
>
> 2. 后面将提供一个具体步骤的详细实例

## 1. 配置说明

1. `BASE_URL`是从公众号预约网页上面抓取下来的，可以外网访问，不是学校官网网页端的接口地址。
2. `SEAT_ID`是某个具体座位的id，需要通过抓包获得。
3. `AreaCode`是区域id，需要通过抓包获得，例如华师大普陀小区一楼B区的id是40。

## 2. 部署方式

1. 非SpringBoot方式：

   首先将项目打包成jar包，然后使用服务器（Linux或者Windows）的定时任务。参考资料：

   - 使用IDEA将项目打包为jar包：[非springboot项目IDEA打包引入第三方jar包](https://blog.csdn.net/zhengaog/article/details/117076358)

   - linux系统可以用crontab实现定时任务：[crontab定时执行java程序(简单易懂)](https://blog.csdn.net/weixin_44422604/article/details/107026556)

   - windows系统可以使用定时执行脚本：[windows设置定时执行脚本](https://www.cnblogs.com/sui776265233/p/13602893.html)

2. SpringBoot方式：

   直接在项目里面配置好定时执行的时间，然后用普通的SpringBoot项目启动方式启动。记得使用`nohup`的守护进程。
   
   - ```shell
     nohup java -jar xxx.jar > /dev/null &
     ```

# 实例

> 以华东师范大学图书馆预约为例，介绍如何一步步使用脚本

1. 首先，通过抓包，获取 API 地址。华师大图书馆公众号里面的预约网页无法在电脑上打开，只能在手机上面打开。所以通过手机端和PC端连接同一个网络，然后借助Fiddler抓包，即可找到 API 地址为 。

2. 获取想要预约的区域id。模拟一次完整的预约过程，其中一个请求的 URL 中有`/areas?tree=1`字段，这个请求的响应体里面包含了区域的id号。例如，可以找到华东师范大学普陀小区一楼B区的区域id号为40。这个40即为`AreaCode`参数的值。

3. 获取想要预约的座位id。在步骤二模拟的一次完整的预约过程中，其中有一个请求的 URL 中包含了`/spaces_old?area=`字段，这个请求的响应体里面就包含了作为的id号。例如，可以找到华东师范大学普陀小区一楼B区138号座位的id为6249。这个6249即为`SEAT_ID`参数的值。

4. 完成上述步骤之后，即完成了关键的配置。接下来将邮箱等内容配置完成即可。

   > 配置邮箱不是必须项。如果不需要接收到邮件通知，需要把`emailInfo`设置为false。

5. 按照教程，将项目打jar包，然后上传到linux服务器，使用crontab设置定时任务，每天凌晨执行。或者直接在maven项目中到处jar包，然后用`nohup`开启项目。

# 提示

- 仅限交流学习使用，请遵守学校各项管理规定、规范制度。
