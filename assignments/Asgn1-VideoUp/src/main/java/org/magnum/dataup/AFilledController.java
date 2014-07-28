/*
 * 
 * Copyright 2014 Jules White
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * 
 */
package org.magnum.dataup;

import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.magnum.dataup.model.Video;
import org.magnum.dataup.model.VideoStatus;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.util.ClassUtils;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.multipart.MultipartFile;

@Controller
public class AFilledController {

	/**
	 * You will need to create one or more Spring controllers to fulfill the
	 * requirements of the assignment. If you use this file, please rename it
	 * to something other than "AnEmptyController"
	 * 
	 * 
		 ________  ________  ________  ________          ___       ___  ___  ________  ___  __       
		|\   ____\|\   __  \|\   __  \|\   ___ \        |\  \     |\  \|\  \|\   ____\|\  \|\  \     
		\ \  \___|\ \  \|\  \ \  \|\  \ \  \_|\ \       \ \  \    \ \  \\\  \ \  \___|\ \  \/  /|_   
		 \ \  \  __\ \  \\\  \ \  \\\  \ \  \ \\ \       \ \  \    \ \  \\\  \ \  \    \ \   ___  \  
		  \ \  \|\  \ \  \\\  \ \  \\\  \ \  \_\\ \       \ \  \____\ \  \\\  \ \  \____\ \  \\ \  \ 
		   \ \_______\ \_______\ \_______\ \_______\       \ \_______\ \_______\ \_______\ \__\\ \__\
		    \|_______|\|_______|\|_______|\|_______|        \|_______|\|_______|\|_______|\|__| \|__|
                                                                                                                                                                                                                                                                        
	 * 
	 */
	
	private VideoFileManager mVideoDataMgr;
	private Map<Long,Video> mVideos = new HashMap<Long, Video>();
	private static final AtomicLong currentId = new AtomicLong(0L);
	
	public AFilledController() throws IOException {
		super();
		mVideoDataMgr = VideoFileManager.get();
	}
	
	@RequestMapping(value = VideoSvcApi.VIDEO_SVC_PATH, method = RequestMethod.GET)
	public @ResponseBody Collection<Video> getVideoList(){
		return mVideos.values();
	}

	/**
	 * Add video meta data to the list and return a created ID.
	 * @param v the video object with meta data. The ID will also be set to this object.
	 * @return the same video object contains new ID. 
	 * TODO: provide information for spring to check
	 */
	@RequestMapping(value = VideoSvcApi.VIDEO_SVC_PATH, method = RequestMethod.POST)
    public @ResponseBody Video addVideo(@RequestBody Video v){
		// call a synchronized method to ensure the access to the list
		v.setDataUrl(getDataUrl(save(v).getId()));
        return v;
    }
	
	/**
	 * Upload the video data to server.
	 * @param id the created id to upload.
	 * @param data video data
	 * @return status of the uploading.
	 * @throws IOException 
	 */
	@RequestMapping(value = VideoSvcApi.VIDEO_DATA_PATH, method = RequestMethod.POST)
	public @ResponseBody VideoStatus setVideoData(
			@PathVariable(VideoSvcApi.ID_PARAMETER) long id, 
			@RequestParam(value = VideoSvcApi.DATA_PARAMETER) MultipartFile data
			) throws IOException{
		Video v = mVideos.get(id);
		mVideoDataMgr.saveVideoData(v, data.getInputStream());
		return new VideoStatus(VideoStatus.VideoState.READY);
	}
	
	/**
	 * Get the video data
	 * @param id of the video
	 * @return response.
	 * @throws IOException 
	 */
	@RequestMapping(value = VideoSvcApi.VIDEO_DATA_PATH, method = RequestMethod.GET)
	public void getData(
			@PathVariable(VideoSvcApi.ID_PARAMETER) long id, 
			HttpServletResponse hsr
			) throws IOException{
		Video v = mVideos.get(id);
		mVideoDataMgr.copyVideoData(v, hsr.getOutputStream());
	}
	
	/**
	 * This method catch all Exceptions of RequestMapping methods and set 404 response code,
	 * which is not always true.
	 * @param ex is caught
	 * @return exception name
	 */
	@ExceptionHandler(Throwable.class)
	@ResponseStatus(value = HttpStatus.NOT_FOUND, reason = "id not exists")
	public String handleAnyException(Throwable ex) {
		return ClassUtils.getShortName(ex.getClass());
	}

// private methods

    private String getDataUrl(long videoId){
        String url = getUrlBaseForLocalServer() + "/video/" + videoId + "/data";
        return url;
    }

    private String getUrlBaseForLocalServer() {
       HttpServletRequest request = 
           ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest();
       String base = 
          "http://"+request.getServerName() 
          + ((request.getServerPort() != 80) ? ":"+request.getServerPort() : "");
       return base;
    }
    
    private Video save(Video entity) {
        checkAndSetId(entity);
        mVideos.put(entity.getId(), entity);
        return entity;
    }

    private void checkAndSetId(Video entity) {
        if(entity.getId() == 0){
            entity.setId(currentId.incrementAndGet());
        }
    }
}