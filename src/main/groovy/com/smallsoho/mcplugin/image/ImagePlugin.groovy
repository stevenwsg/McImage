package com.smallsoho.mcplugin.image

import com.android.build.gradle.AppPlugin
import com.smallsoho.mcplugin.image.models.Config
import com.smallsoho.mcplugin.image.utils.CompressUtil
import com.smallsoho.mcplugin.image.utils.SizeUtil

import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project

class ImagePlugin implements Plugin<Project> {

    @Override
    void apply(Project project) {

        def hasApp = project.plugins.withType(AppPlugin)
        def variants = hasApp ? project.android.applicationVariants : project.android.libraryVariants
        project.extensions.create('McImageConfig', Config)

        project.afterEvaluate {
            variants.all { variant ->

                def imgDir
                if (variant.productFlavors.size() == 0) {
                    imgDir = 'merged'
                } else {
                    imgDir = "merged/${variant.productFlavors[0].name}"
                }

                Config config = project.McImageConfig

                //if don't need this plugin
                if (!config.isCompress && !config.isCheck) {
                    return
                }

                def processResourceTask = project.tasks.findByName("process${variant.name.capitalize()}Resources")
                def mcPicPlugin = "McImage${variant.name.capitalize()}"
                project.task(mcPicPlugin) << {

                    String resPath = "${project.projectDir}/build/intermediates/res/${imgDir}/"

                    def dir = new File("${resPath}")

                    ArrayList<String> bigImgList = new ArrayList<>()

                    dir.eachDir() { channelDir ->
                        channelDir.eachDir { drawDir ->
                            def file = new File("${drawDir}")
                            if (file.name.contains('drawable') || file.name.contains('mipmap')) {
                                file.eachFile { imgFile ->

                                    if (config.isCheck && SizeUtil.isBigImage(imgFile, config.maxSize)) {
                                        bigImgList.add(file.getPath() + file.getName())
                                    }
                                    if (config.isCompress) {
                                        CompressUtil.compressImg(imgFile, project.projectDir)
                                    }

                                }
                            }
                        }
                    }

                    if (bigImgList.size() != 0) {
                        StringBuffer stringBuffer = new StringBuffer("You have big Img!!!! \n")
                        for (int i = 0; i < bigImgList.size(); i++) {
                            stringBuffer.append(bigImgList.get(i))
                            stringBuffer.append("\n")
                        }
                        throw new GradleException(stringBuffer.toString())
                    }

                }

                //inject plugin
                project.tasks.findByName(mcPicPlugin).dependsOn processResourceTask.taskDependencies.getDependencies(processResourceTask)
                processResourceTask.dependsOn project.tasks.findByName(mcPicPlugin)
            }
        }
    }

}