package com.dsqd.amc.linkedmo.model;

import net.minidev.json.JSONObject;

import java.time.LocalDateTime;

public class Reply {
    private int id;
    private int boardId;
    private int parentReplyId;
    private String content;
    private String userId;
    private String userName;
    private LocalDateTime createdAt;

    private Reply(Builder builder) {
        this.id = builder.id;
        this.boardId = builder.boardId;
        this.parentReplyId = builder.parentReplyId;
        this.content = builder.content;
        this.userId = builder.userId;
        this.userName = builder.userName;
        this.createdAt = builder.createdAt;
    }

    public static class Builder {
        private int id;
        private int boardId;
        private int parentReplyId;
        private String content;
        private String userId;
        private String userName;
        private LocalDateTime createdAt;

        public Builder id(int id) {
            this.id = id;
            return this;
        }

        public Builder boardId(int boardId) {
            this.boardId = boardId;
            return this;
        }

        public Builder parentReplyId(int parentReplyId) {
            this.parentReplyId = parentReplyId;
            return this;
        }

        public Builder content(String content) {
            this.content = content;
            return this;
        }

        public Builder userId(String userId) {
            this.userId = userId;
            return this;
        }

        public Builder userName(String userName) {
            this.userName = userName;
            return this;
        }

        public Builder createdAt(LocalDateTime createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public Reply build() {
            return new Reply(this);
        }
    }

    public void parse(JSONObject jsonObject) {
        this.content = (String) jsonObject.get("content");
        this.userId = (String) jsonObject.get("userId");
        this.userName = (String) jsonObject.get("userName");
        this.parentReplyId = jsonObject.getAsNumber("parentReplyId").intValue();
    }

    public String toJSONString() {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("id", id);
        jsonObject.put("boardId", boardId);
        jsonObject.put("parentReplyId", parentReplyId);
        jsonObject.put("content", content);
        jsonObject.put("userId", userId);
        jsonObject.put("userName", userName);
        jsonObject.put("createdAt", createdAt.toString());
        return jsonObject.toJSONString();
    }

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public int getBoardId() {
		return boardId;
	}

	public void setBoardId(int boardId) {
		this.boardId = boardId;
	}

	public int getParentReplyId() {
		return parentReplyId;
	}

	public void setParentReplyId(int parentReplyId) {
		this.parentReplyId = parentReplyId;
	}

	public String getContent() {
		return content;
	}

	public void setContent(String content) {
		this.content = content;
	}

	public String getUserId() {
		return userId;
	}

	public void setUserId(String userId) {
		this.userId = userId;
	}

	public String getUserName() {
		return userName;
	}

	public void setUserName(String userName) {
		this.userName = userName;
	}

	public LocalDateTime getCreatedAt() {
		return createdAt;
	}

	public void setCreatedAt(LocalDateTime createdAt) {
		this.createdAt = createdAt;
	}


}
