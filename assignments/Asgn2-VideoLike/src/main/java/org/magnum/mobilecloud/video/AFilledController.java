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

package org.magnum.mobilecloud.video;

import java.security.Principal;
import java.util.Collection;
import java.util.List;

import javax.servlet.http.HttpServletResponse;

import org.magnum.mobilecloud.video.client.VideoSvcApi;
import org.magnum.mobilecloud.video.repository.Video;
import org.magnum.mobilecloud.video.repository.VideoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.rest.webmvc.ResourceNotFoundException;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import com.google.common.collect.Lists;

@Controller
public class AFilledController {
	
	private static final String VIDEO_ID_PATH = VideoSvcApi.VIDEO_SVC_PATH + "/{id}";
	private static final String VIDEO_LIKE_PATH = VIDEO_ID_PATH + "/like";
	private static final String VIDEO_UNLIKE_PATH = VIDEO_ID_PATH + "/unlike";
	private static final String VIDEO_LIKEDBY_PATH = VIDEO_ID_PATH + "/likedby";
	private static final String VIDEO_ID = "id";
	// The VideoRepository that we are going to store our videos
	// in. We don't explicitly construct a VideoRepository, but
	// instead mark this object as a dependency that needs to be
	// injected by Spring. Our Application class has a method
	// annotated with @Bean that determines what object will end
	// up being injected into this member variable.
	//
	// Also notice that we don't even need a setter for Spring to
	// do the injection.
	//
	@Autowired
	private VideoRepository videos;
	
	@RequestMapping(value=VideoSvcApi.VIDEO_SVC_PATH, method=RequestMethod.GET)
	public @ResponseBody Collection<Video> getVideoList(){
		return Lists.newArrayList(videos.findAll());
	}
	
	@RequestMapping(value=VideoSvcApi.VIDEO_SVC_PATH, method=RequestMethod.POST)
	public @ResponseBody Video addVideo(@RequestBody Video v){
		 return videos.save(v);
	}
	
	@RequestMapping(value=VIDEO_ID_PATH, method=RequestMethod.GET)
	public @ResponseBody Video getVideo(@PathVariable long id){
		return videos.findOne(id);
	}
		
	@RequestMapping(value=VIDEO_LIKE_PATH, method=RequestMethod.POST)
	public void likeVideo(@PathVariable(VIDEO_ID) long id, HttpServletResponse response, Principal p){
		setLike(id, p.getName(), response, true);
	}
	
	@RequestMapping(value=VIDEO_UNLIKE_PATH, method=RequestMethod.POST)
	public void unlikeVideo(@PathVariable(VIDEO_ID) long id, HttpServletResponse response, Principal p){
		setLike(id, p.getName(), response, false);
	}
	
	@RequestMapping(value=VIDEO_LIKEDBY_PATH, method=RequestMethod.GET)
	public @ResponseBody List<String> likedbyVideo(@PathVariable(VIDEO_ID) long id) {
		Video v = videos.findOne(id);
		if (v == null){
			throw new ResourceNotFoundException();
		} 
		return v.getLikedUsers();
	}
	
	// private methods
	
	private void setLike(long id, String user, HttpServletResponse res, boolean isLike){
		Video v = videos.findOne(id);
		int code = HttpServletResponse.SC_OK;
		if (v==null){
			code = HttpServletResponse.SC_NOT_FOUND;
		} else {
			if (isLike == v.isLiked(user)){
				code = HttpServletResponse.SC_BAD_REQUEST;
			} else {
				if (isLike)
					v.addLikedUser(user);
				else
					v.removeLikedUser(user);
				videos.save(v);
			}
		}
		res.setStatus(code);
	}
	
}
