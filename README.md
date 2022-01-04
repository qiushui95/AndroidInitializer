# AndroidInitializer[![](https://jitpack.io/v/qiushui95/AndroidInitializer.svg)](https://jitpack.io/#qiushui95/AndroidInitializer)
Android自动初始化框架  
多module自动初始化
## 安装
``` kotlin dsl
	dependencies {
	        implementation("com.github.qiushui95:AndroidInitializer:1.0.7")
	}
```
## 继承
``` kotlin
class SampleInitializer : AndroidInitializer<String>() {
    override val id: String = super.id
    override val parentIdList: List<String> = super.parentIdList
    override val dispatcher: CoroutineDispatcher = super.dispatcher
    override val needBlockingMain: Boolean = super.needBlockingMain

    override fun onParentCompleted(parentId: String, result: Any) {
        super.onParentCompleted(parentId, result)
    }

    override fun onAllChildrenCompleted() {
        super.onAllChildrenCompleted()
    }

    override fun doInit(): String {
        Thread.sleep(3000)

        return "SampleInitializer.doInit执行完毕"
    }
}
```
继承AndroidInitializer,重写doInit方法,返回值会传递给子任务.
## Initializer说明
- ### id  
    当前Initializer的唯一标识,用于子任务依赖父任务.
- ### parentIdList  
    需要依赖的父任务id列表,当所有父任务完成后才会执行子任务.
- ### dispatcher
    协程调度器,可以指定线程执行
- ### needBlockingMain
    是否需要阻塞主线程直到该任务结束.
- ### onParentCompleted
    父任务完成回调
    参数parentId,父任务id
    参数result,父任务初始化结果
- ### onAllChildrenCompleted
    所有子任务完成回调
- ### doInit
    初始化任务块,返回值会传递给子任务.
## 定义
在AndroidManifest.xml中定义provider
``` 
        <provider
            android:name="son.ysy.initializer.android.provider.StartupProvider"
            android:authorities="${applicationId}.androidx_start"
            android:exported="false">

            <meta-data
                android:name="${packageName}.initializer.SampleInitializer"
                android:value="@string/initializer_start_up" />
        </provider>          
```
有多少Initializer就定义多少个<meta-data>